/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.BuiltinSymbolsBase
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.evaluate.evaluateConstants
import org.jetbrains.kotlin.fir.backend.generators.*
import org.jetbrains.kotlin.fir.backend.generators.DataClassMembersGenerator
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.declarations.utils.isSynthetic
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.extensions.declarationGenerators
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.generatedMembers
import org.jetbrains.kotlin.fir.extensions.generatedNestedClassifiers
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.signaturer.FirBasedSignatureComposer
import org.jetbrains.kotlin.fir.signaturer.FirMangler
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.PsiIrFileEntry
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.generators.GeneratorExtensions
import org.jetbrains.kotlin.psi2ir.generators.generateTypicalIrProviderList
import org.jetbrains.kotlin.resolve.BindingContext

class Fir2IrConverter(
    private val moduleDescriptor: FirModuleDescriptor,
    private val components: Fir2IrComponents
) : Fir2IrComponents by components {

    private val generatorExtensions = session.extensionService.declarationGenerators

    private var wereSourcesFakeOverridesBound = false
    private val postponedDeclarationsForFakeOverridesBinding = mutableListOf<IrDeclaration>()

    fun runSourcesConversion(
        allFirFiles: List<FirFile>,
        irModuleFragment: IrModuleFragmentImpl,
        irGenerationExtensions: Collection<IrGenerationExtension>,
        fir2irVisitor: Fir2IrVisitor,
        languageVersionSettings: LanguageVersionSettings,
        descriptorMangler: KotlinMangler.DescriptorMangler,
        extensions: StubGeneratorExtensions,
    ) {
        for (firFile in allFirFiles) {
            registerFileAndClasses(firFile, irModuleFragment)
        }

        val irProviders =
            generateTypicalIrProviderList(
                irModuleFragment.descriptor, irBuiltIns, symbolTable,
                descriptorFinder = DescriptorByIdSignatureFinderImpl(irModuleFragment.descriptor, descriptorMangler),
                extensions = extensions
            )
        val externalDependenciesGenerator = ExternalDependenciesGenerator(
            symbolTable,
            irProviders
        )

        // Necessary call to generate built-in IR classes
        externalDependenciesGenerator.generateUnboundSymbolsAsDependencies()
        classifierStorage.preCacheBuiltinClasses()
        // The file processing is performed phase-to-phase:
        //   1. Creation of all non-local regular classes
        //   2. Class header processing (type parameters, supertypes, this receiver)
        for (firFile in allFirFiles) {
            processClassHeaders(firFile)
        }
        //   3. Class member (functions/properties/constructors) processing. This doesn't involve bodies (only types).
        //   If we encounter local class / anonymous object in signature, then (1) and (2) is performed immediately for this class,
        //   and (3) and (4) a bit later
        for (firFile in allFirFiles) {
            processFileAndClassMembers(firFile)
        }
        //   4. Override processing which sets overridden symbols for everything inside non-local regular classes
        for (firFile in allFirFiles) {
            bindFakeOverridesInFile(firFile)
        }

        wereSourcesFakeOverridesBound = true
        fakeOverrideGenerator.bindOverriddenSymbols(postponedDeclarationsForFakeOverridesBinding)
        postponedDeclarationsForFakeOverridesBinding.clear()

        //   Do (3) and (4) for local classes encountered during (3)
        classifierStorage.processMembersOfClassesCreatedOnTheFly()

        //   5. Body processing
        //   If we encounter local class / anonymous object here, then we perform all (1)-(5) stages immediately
        delegatedMemberGenerator.generateBodies()
        for (firFile in allFirFiles) {
            firFile.accept(fir2irVisitor, null)
        }

        externalDependenciesGenerator.generateUnboundSymbolsAsDependencies()
        val stubGenerator = irProviders.filterIsInstance<DeclarationStubGenerator>().first()
        irModuleFragment.acceptVoid(ExternalPackageParentPatcher(stubGenerator))

        evaluateConstants(irModuleFragment)

        if (irGenerationExtensions.isNotEmpty()) {
            val pluginContext = Fir2IrPluginContext(
                languageVersionSettings,
                BuiltinSymbolsBase(irBuiltIns, symbolTable),
                session.moduleData.platform,
                irBuiltIns,
                symbolTable
            )
            for (extension in irGenerationExtensions) {
                extension.generate(irModuleFragment, pluginContext)
            }
        }

    }

    fun bindFakeOverridesOrPostpone(declarations: List<IrDeclaration>) {
        // Do not run binding for lazy classes until all sources declarations are processed
        if (wereSourcesFakeOverridesBound) {
            fakeOverrideGenerator.bindOverriddenSymbols(declarations)
        } else {
            postponedDeclarationsForFakeOverridesBinding += declarations
        }
    }

    fun processLocalClassAndNestedClassesOnTheFly(regularClass: FirRegularClass, parent: IrDeclarationParent): IrClass {
        val irClass = registerClassAndNestedClasses(regularClass, parent)
        processClassAndNestedClassHeaders(regularClass)
        return irClass
    }

    fun processAnonymousObjectOnTheFly(anonymousObject: FirAnonymousObject, irClass: IrClass) {
        registerNestedClasses(anonymousObject, irClass)
        processNestedClassHeaders(anonymousObject)
    }

    fun processLocalClassAndNestedClasses(regularClass: FirRegularClass, parent: IrDeclarationParent): IrClass {
        val irClass = registerClassAndNestedClasses(regularClass, parent)
        processClassAndNestedClassHeaders(regularClass)
        processClassMembers(regularClass, irClass)
        bindFakeOverridesInClass(irClass)
        return irClass
    }

    private fun registerFileAndClasses(file: FirFile, moduleFragment: IrModuleFragment) {
        val fileEntry = when (file.origin) {
            FirDeclarationOrigin.Source ->
                file.psi?.let { PsiIrFileEntry(it as KtFile) } ?: NaiveSourceBasedFileEntryImpl(file.path ?: file.name)
            FirDeclarationOrigin.Synthetic -> NaiveSourceBasedFileEntryImpl(file.name)
            else -> error("Unsupported file origin: ${file.origin}")
        }
        val irFile = IrFileImpl(
            fileEntry,
            moduleDescriptor.getPackage(file.packageFqName).fragments.first(),
            moduleFragment
        )
        declarationStorage.registerFile(file, irFile)
        file.declarations.forEach {
            if (it is FirRegularClass) {
                registerClassAndNestedClasses(it, irFile)
            }
        }
        moduleFragment.files += irFile
    }

    fun processClassHeaders(file: FirFile) {
        file.declarations.forEach {
            when (it) {
                is FirRegularClass -> processClassAndNestedClassHeaders(it)
                is FirTypeAlias -> classifierStorage.registerTypeAlias(it, declarationStorage.getIrFile(file))
                else -> {}
            }
        }
    }

    private fun processFileAndClassMembers(file: FirFile) {
        val irFile = declarationStorage.getIrFile(file)
        for (declaration in file.declarations) {
            val irDeclaration = processMemberDeclaration(declaration, null, irFile) ?: continue
            irFile.declarations += irDeclaration
        }
    }

    fun processAnonymousObjectMembers(
        anonymousObject: FirAnonymousObject,
        irClass: IrClass,
        processHeaders: Boolean
    ): IrClass {
        if (processHeaders) {
            registerNestedClasses(anonymousObject, irClass)
            processNestedClassHeaders(anonymousObject)
        }
        anonymousObject.primaryConstructorIfAny(session)?.let {
            irClass.declarations += declarationStorage.createIrConstructor(
                it.fir, irClass, isLocal = true
            )
        }
        for (declaration in syntheticPropertiesLast(anonymousObject.declarations)) {
            val irDeclaration = processMemberDeclaration(declaration, anonymousObject, irClass) ?: continue
            irClass.declarations += irDeclaration
        }
        // Add delegated members *before* fake override generations.
        // Otherwise, fake overrides for delegated members, which are redundant, will be added.
        val realDeclarations = delegatedMembers(irClass) + anonymousObject.declarations
        with(fakeOverrideGenerator) {
            irClass.addFakeOverrides(anonymousObject, realDeclarations)
        }
        bindFakeOverridesInClass(irClass)

        return irClass
    }

    internal fun processClassMembers(
        regularClass: FirRegularClass,
        irClass: IrClass = classifierStorage.getCachedIrClass(regularClass)!!
    ): IrClass {
        val allDeclarations = mutableListOf<FirDeclaration>().apply {
            addAll(regularClass.declarations.toMutableList())
            if (generatorExtensions.isNotEmpty()) {
                addAll(regularClass.generatedMembers(session))
                addAll(regularClass.generatedNestedClassifiers(session))
            }
        }
        val irConstructor = (allDeclarations.firstOrNull { it is FirConstructor && it.isPrimary })?.let {
            declarationStorage.getOrCreateIrConstructor(it as FirConstructor, irClass, isLocal = regularClass.isLocal)
        }
        if (irConstructor != null) {
            irClass.declarations += irConstructor
        }
        for (declaration in syntheticPropertiesLast(allDeclarations)) {
            val irDeclaration = processMemberDeclaration(declaration, regularClass, irClass) ?: continue
            irClass.declarations += irDeclaration
        }
        // Add delegated members *before* fake override generations.
        // Otherwise, fake overrides for delegated members, which are redundant, will be added.
        allDeclarations += delegatedMembers(irClass)
        // Add synthetic members *before* fake override generations.
        // Otherwise, redundant members, e.g., synthetic toString _and_ fake override toString, will be added.
        if (irConstructor != null && (irClass.isValue || irClass.isData)) {
            declarationStorage.enterScope(irConstructor)
            val dataClassMembersGenerator = DataClassMembersGenerator(components)
            if (irClass.isSingleFieldValueClass) {
                allDeclarations += dataClassMembersGenerator.generateSingleFieldValueClassMembers(regularClass, irClass)
            }
            if (irClass.isMultiFieldValueClass) {
                allDeclarations += dataClassMembersGenerator.generateMultiFieldValueClassMembers(regularClass, irClass)
            }
            if (irClass.isData) {
                allDeclarations += dataClassMembersGenerator.generateDataClassMembers(regularClass, irClass)
            }
            declarationStorage.leaveScope(irConstructor)
        }
        with(fakeOverrideGenerator) {
            irClass.addFakeOverrides(regularClass, allDeclarations)
        }

        return irClass
    }

    fun bindFakeOverridesInFile(file: FirFile) {
        val irFile = declarationStorage.getIrFile(file)
        for (irDeclaration in irFile.declarations) {
            if (irDeclaration is IrClass) {
                bindFakeOverridesInClass(irDeclaration)
            }
        }
    }

    fun bindFakeOverridesInClass(klass: IrClass) {
        fakeOverrideGenerator.bindOverriddenSymbols(klass.declarations)
        delegatedMemberGenerator.bindDelegatedMembersOverriddenSymbols(klass)
        for (irDeclaration in klass.declarations) {
            if (irDeclaration is IrClass) {
                bindFakeOverridesInClass(irDeclaration)
            }
        }
    }

    private fun delegatedMembers(irClass: IrClass): List<FirDeclaration> {
        return irClass.declarations.filter {
            it.origin == IrDeclarationOrigin.DELEGATED_MEMBER
        }.mapNotNull {
            components.declarationStorage.originalDeclarationForDelegated(it)
        }
    }

    // Sort declarations so that all non-synthetic declarations are before synthetic ones.
    // This is needed because converting synthetic fields for implementation delegation needs to know
    // existing declarations in the class to avoid adding redundant delegated members.
    private fun syntheticPropertiesLast(declarations: List<FirDeclaration>): Iterable<FirDeclaration> {
        return declarations.sortedBy { it !is FirField && it.isSynthetic }
    }

    private fun registerClassAndNestedClasses(regularClass: FirRegularClass, parent: IrDeclarationParent): IrClass {
        // Local classes might be referenced before they declared (see usages of Fir2IrClassifierStorage.createLocalIrClassOnTheFly)
        // So, we only need to set its parent properly
        val irClass =
            classifierStorage.getCachedIrClass(regularClass)?.apply {
                this.parent = parent
            } ?: classifierStorage.registerIrClass(regularClass, parent)
        registerNestedClasses(regularClass, irClass)
        return irClass
    }

    private fun registerNestedClasses(klass: FirClass, irClass: IrClass) {
        klass.declarations.forEach {
            if (it is FirRegularClass) {
                registerClassAndNestedClasses(it, irClass)
            }
        }
        if (generatorExtensions.isNotEmpty()) {
            klass.generatedNestedClassifiers(session).forEach {
                if (it is FirRegularClass) {
                    registerClassAndNestedClasses(it, irClass)
                }
            }
        }
    }

    private fun processClassAndNestedClassHeaders(regularClass: FirRegularClass) {
        classifierStorage.processClassHeader(regularClass)
        processNestedClassHeaders(regularClass)
    }

    private fun processNestedClassHeaders(klass: FirClass) {
        klass.declarations.forEach {
            if (it is FirRegularClass) {
                processClassAndNestedClassHeaders(it)
            }
        }
        if (generatorExtensions.isNotEmpty()) {
            klass.generatedNestedClassifiers(session).forEach {
                if (it is FirRegularClass) {
                    processClassAndNestedClassHeaders(it)
                }
            }
        }
    }

    private fun processMemberDeclaration(
        declaration: FirDeclaration,
        containingClass: FirClass?,
        parent: IrDeclarationParent
    ): IrDeclaration? {
        val isLocal = containingClass != null &&
                (containingClass !is FirRegularClass || containingClass.isLocal)
        return when (declaration) {
            is FirRegularClass -> {
                processClassMembers(declaration)
            }
            is FirSimpleFunction -> {
                declarationStorage.getOrCreateIrFunction(
                    declaration, parent, isLocal = isLocal
                )
            }
            is FirProperty -> {
                declarationStorage.getOrCreateIrProperty(
                    declaration, parent, isLocal = isLocal
                )
            }
            is FirField -> {
                if (declaration.isSynthetic) {
                    declarationStorage.createIrFieldAndDelegatedMembers(declaration, containingClass!!, parent as IrClass)
                } else {
                    throw AssertionError("Unexpected non-synthetic field: ${declaration::class}")
                }
            }
            is FirConstructor -> if (!declaration.isPrimary) {
                declarationStorage.getOrCreateIrConstructor(
                    declaration, parent as IrClass, isLocal = isLocal
                )
            } else {
                null
            }
            is FirEnumEntry -> {
                classifierStorage.createIrEnumEntry(declaration, parent as IrClass)
            }
            is FirAnonymousInitializer -> {
                declarationStorage.createIrAnonymousInitializer(declaration, parent as IrClass)
            }
            is FirTypeAlias -> {
                // DO NOTHING
                null
            }
            else -> {
                error("Unexpected member: ${declaration::class}")
            }
        }
    }

    companion object {
        @OptIn(ObsoleteDescriptorBasedAPI::class)
        fun createModuleFragment(
            session: FirSession,
            scopeSession: ScopeSession,
            firFiles: List<FirFile>,
            languageVersionSettings: LanguageVersionSettings,
            descriptorMangler: KotlinMangler.DescriptorMangler,
            signaturer: IdSignatureComposer,
            generatorExtensions: GeneratorExtensions,
            mangler: FirMangler,
            irFactory: IrFactory,
            visibilityConverter: Fir2IrVisibilityConverter,
            specialSymbolProvider: Fir2IrSpecialSymbolProvider?,
            irGenerationExtensions: Collection<IrGenerationExtension>
        ): Fir2IrResult {
            val moduleDescriptor = FirModuleDescriptor(session)
            val symbolTable = SymbolTable(signaturer, irFactory)
            val signatureComposer = FirBasedSignatureComposer(mangler)
            val components = Fir2IrComponentsStorage(session, scopeSession, symbolTable, irFactory, signatureComposer)
            val converter = Fir2IrConverter(moduleDescriptor, components)

            components.converter = converter

            val classifierStorage = Fir2IrClassifierStorage(components)
            components.classifierStorage = classifierStorage
            components.delegatedMemberGenerator = DelegatedMemberGenerator(components)
            val declarationStorage = Fir2IrDeclarationStorage(components, moduleDescriptor)
            components.declarationStorage = declarationStorage
            components.visibilityConverter = visibilityConverter
            val typeConverter = Fir2IrTypeConverter(components)
            components.typeConverter = typeConverter
            val irBuiltIns =
                IrBuiltInsOverFir(
                    components, languageVersionSettings, moduleDescriptor,
                    languageVersionSettings.getFlag(AnalysisFlags.builtInsFromSources)
                )
            components.irBuiltIns = irBuiltIns
            val conversionScope = Fir2IrConversionScope()
            val fir2irVisitor = Fir2IrVisitor(components, conversionScope)
            val builtIns = Fir2IrBuiltIns(components, specialSymbolProvider)
            val annotationGenerator = AnnotationGenerator(components)
            components.builtIns = builtIns
            components.annotationGenerator = annotationGenerator
            val fakeOverrideGenerator = FakeOverrideGenerator(components, conversionScope)
            components.fakeOverrideGenerator = fakeOverrideGenerator
            val callGenerator = CallAndReferenceGenerator(components, fir2irVisitor, conversionScope)
            components.callGenerator = callGenerator

            val irModuleFragment = IrModuleFragmentImpl(moduleDescriptor, irBuiltIns)

            val allFirFiles = buildList {
                addAll(firFiles)
                addAll(session.createFilesWithGeneratedDeclarations())
            }

            converter.runSourcesConversion(
                allFirFiles, irModuleFragment, irGenerationExtensions, fir2irVisitor, languageVersionSettings,
                descriptorMangler, generatorExtensions
            )

            return Fir2IrResult(irModuleFragment, symbolTable, components)
        }
    }

    private class Fir2IrPluginContext(
        override val languageVersionSettings: LanguageVersionSettings,
        override val symbols: BuiltinSymbolsBase,
        override val platform: TargetPlatform?,
        override val irBuiltIns: IrBuiltIns,
        @property:ObsoleteDescriptorBasedAPI
        override val symbolTable: SymbolTable
    ) : IrPluginContext {
        @ObsoleteDescriptorBasedAPI
        override val moduleDescriptor: ModuleDescriptor
            get() = error("Should not be called")

        @ObsoleteDescriptorBasedAPI
        override val bindingContext: BindingContext
            get() = error("Should not be called")

        @ObsoleteDescriptorBasedAPI
        override val typeTranslator: TypeTranslator
            get() = error("Should not be called")

        override fun createDiagnosticReporter(pluginId: String): IrMessageLogger {
            error("Should not be called")
        }

        override fun referenceClass(fqName: FqName): IrClassSymbol? {
            error("Should not be called")
        }

        override fun referenceTypeAlias(fqName: FqName): IrTypeAliasSymbol? {
            error("Should not be called")
        }

        override fun referenceConstructors(classFqn: FqName): Collection<IrConstructorSymbol> {
            error("Should not be called")
        }

        override fun referenceFunctions(fqName: FqName): Collection<IrSimpleFunctionSymbol> {
            error("Should not be called")
        }

        override fun referenceProperties(fqName: FqName): Collection<IrPropertySymbol> {
            error("Should not be called")
        }

        override fun referenceTopLevel(
            signature: IdSignature,
            kind: IrDeserializer.TopLevelSymbolKind,
            moduleDescriptor: ModuleDescriptor
        ): IrSymbol? {
            error("Should not be called")
        }
    }
}
