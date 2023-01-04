/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtPsiSourceFileLinesMapping
import org.jetbrains.kotlin.KtSourceFileLinesMappingFromLineStartOffsets
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.fir.*
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
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.signaturer.FirBasedSignatureComposer
import org.jetbrains.kotlin.fir.signaturer.FirMangler
import org.jetbrains.kotlin.fir.symbols.lazyDeclarationResolver
import org.jetbrains.kotlin.ir.PsiIrFileEntry
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.checker.EvaluationMode
import org.jetbrains.kotlin.ir.interpreter.checker.IrConstTransformer
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.psi.KtFile

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
        fir2IrExtensions: Fir2IrExtensions,
    ) {
        session.lazyDeclarationResolver.disableLazyResolveContractChecks()
        for (firFile in allFirFiles) {
            registerFileAndClasses(firFile, irModuleFragment)
        }
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
            withFileAnalysisExceptionWrapping(firFile) {
                firFile.accept(fir2irVisitor, null)
            }
        }

        evaluateConstants(irModuleFragment)

        if (irGenerationExtensions.isNotEmpty()) {
            val pluginContext = Fir2IrPluginContext(components, irModuleFragment.descriptor)
            for (extension in irGenerationExtensions) {
                extension.generate(irModuleFragment, pluginContext)
            }
        }

        irModuleFragment.acceptVoid(ExternalPackageParentPatcher(components, fir2IrExtensions))
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
                file.psi?.let { PsiIrFileEntry(it as KtFile) }
                    ?: when (val linesMapping = file.sourceFileLinesMapping) {
                        is KtSourceFileLinesMappingFromLineStartOffsets ->
                            NaiveSourceBasedFileEntryImpl(
                                file.sourceFile?.path ?: file.sourceFile?.name ?: file.name,
                                linesMapping.lineStartOffsets,
                                linesMapping.lastOffset
                            )
                        is KtPsiSourceFileLinesMapping -> PsiIrFileEntry(linesMapping.psiFile)
                        else ->
                            NaiveSourceBasedFileEntryImpl(file.sourceFile?.path ?: file.sourceFile?.name ?: file.name)
                    }
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

    private fun processClassHeaders(file: FirFile) {
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
        // At least on enum entry creation we may need a default constructor, so ctors should be converted first
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

    private fun bindFakeOverridesInFile(file: FirFile) {
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
    private fun syntheticPropertiesLast(declarations: Iterable<FirDeclaration>): Iterable<FirDeclaration> {
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
            is FirScript -> {
                assert(parent is IrFile)
                declarationStorage.getOrCreateIrScript(declaration).also { irScript ->
                    declarationStorage.enterScope(irScript)
                    for (scriptStatement in declaration.statements) {
                        if (scriptStatement is FirDeclaration) {
                            processMemberDeclaration(scriptStatement, null, irScript)
                        }
                    }
                    declarationStorage.leaveScope(irScript)
                }
            }
            is FirSimpleFunction -> {
                declarationStorage.getOrCreateIrFunction(
                    declaration, parent, isLocal = isLocal
                )
            }
            is FirProperty -> {
                if (declaration.source?.kind == KtFakeSourceElementKind.EnumGeneratedDeclaration &&
                    declaration.name == StandardNames.ENUM_ENTRIES &&
                    !session.languageVersionSettings.supportsFeature(LanguageFeature.EnumEntries)
                ) {
                    // Note: we have to do it, because backend without the feature
                    // cannot process Enum.entries properly
                    null
                } else {
                    declarationStorage.getOrCreateIrProperty(
                        declaration, parent, isLocal = isLocal
                    )
                }
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
        private fun evaluateConstants(irModuleFragment: IrModuleFragment) {
            val interpreter = IrInterpreter(irModuleFragment.irBuiltins)
            irModuleFragment.files.forEach {
                it.transformChildren(IrConstTransformer(interpreter, it, mode = EvaluationMode.ONLY_BUILTINS), null)
            }
        }

        fun createModuleFragmentWithSignaturesIfNeeded(
            session: FirSession,
            scopeSession: ScopeSession,
            firFiles: List<FirFile>,
            languageVersionSettings: LanguageVersionSettings,
            signaturer: IdSignatureComposer,
            fir2IrExtensions: Fir2IrExtensions,
            mangler: FirMangler,
            irMangler: KotlinMangler.IrMangler,
            irFactory: IrFactory,
            visibilityConverter: Fir2IrVisibilityConverter,
            specialSymbolProvider: Fir2IrSpecialSymbolProvider,
            irGenerationExtensions: Collection<IrGenerationExtension>,
            generateSignatures: Boolean,
            kotlinBuiltIns: KotlinBuiltIns,
            dependentComponents: List<Fir2IrComponents>,
            currentSymbolTable: SymbolTable?
        ): Fir2IrResult {
            if (!generateSignatures) {
                return createModuleFragmentWithoutSignatures(
                    session, scopeSession, firFiles, languageVersionSettings,
                    fir2IrExtensions, mangler, irMangler, irFactory,
                    visibilityConverter, specialSymbolProvider, irGenerationExtensions,
                    kotlinBuiltIns, dependentComponents, currentSymbolTable
                )
            }
            val signatureComposer = FirBasedSignatureComposer(
                mangler,
                dependentComposers = dependentComponents.map { it.signatureComposer as FirBasedSignatureComposer }
            )
            val symbolTable = createSymbolTable(signaturer, signatureComposer, irFactory, currentSymbolTable)
            return createModuleFragmentWithSymbolTable(
                session, scopeSession, firFiles, languageVersionSettings,
                fir2IrExtensions, irMangler, irFactory, visibilityConverter,
                specialSymbolProvider, irGenerationExtensions, signatureComposer,
                symbolTable, generateSignatures = true, kotlinBuiltIns = kotlinBuiltIns, dependentComponents = dependentComponents
            )
        }

        fun createModuleFragmentWithoutSignatures(
            session: FirSession,
            scopeSession: ScopeSession,
            firFiles: List<FirFile>,
            languageVersionSettings: LanguageVersionSettings,
            fir2IrExtensions: Fir2IrExtensions,
            mangler: FirMangler,
            irMangler: KotlinMangler.IrMangler,
            irFactory: IrFactory,
            visibilityConverter: Fir2IrVisibilityConverter,
            specialSymbolProvider: Fir2IrSpecialSymbolProvider,
            irGenerationExtensions: Collection<IrGenerationExtension>,
            kotlinBuiltIns: KotlinBuiltIns,
            dependentComponents: List<Fir2IrComponents>,
            currentSymbolTable: SymbolTable?
        ): Fir2IrResult {
            val signatureComposer = FirBasedSignatureComposer(mangler, dependentComposers = dependentComponents.map { it.signatureComposer as FirBasedSignatureComposer })
            val signaturer = DescriptorSignatureComposerStub()
            val symbolTable = createSymbolTable(signaturer, signatureComposer, irFactory, currentSymbolTable)
            return createModuleFragmentWithSymbolTable(
                session, scopeSession, firFiles, languageVersionSettings,
                fir2IrExtensions, irMangler, irFactory, visibilityConverter,
                specialSymbolProvider, irGenerationExtensions, signatureComposer,
                symbolTable, generateSignatures = false, kotlinBuiltIns = kotlinBuiltIns, dependentComponents = dependentComponents
            )
        }

        private fun createSymbolTable(
            signaturer: IdSignatureComposer,
            signatureComposer: FirBasedSignatureComposer,
            irFactory: IrFactory,
            currentSymbolTable: SymbolTable?
        ): SymbolTable =
            currentSymbolTable ?: SymbolTable(
                signaturer = WrappedDescriptorSignatureComposer(signaturer, signatureComposer),
                irFactory = irFactory
            )

        private fun createModuleFragmentWithSymbolTable(
            session: FirSession,
            scopeSession: ScopeSession,
            firFiles: List<FirFile>,
            languageVersionSettings: LanguageVersionSettings,
            fir2IrExtensions: Fir2IrExtensions,
            irMangler: KotlinMangler.IrMangler,
            irFactory: IrFactory,
            visibilityConverter: Fir2IrVisibilityConverter,
            specialSymbolProvider: Fir2IrSpecialSymbolProvider,
            irGenerationExtensions: Collection<IrGenerationExtension>,
            signatureComposer: FirBasedSignatureComposer,
            symbolTable: SymbolTable,
            generateSignatures: Boolean,
            kotlinBuiltIns: KotlinBuiltIns,
            dependentComponents: List<Fir2IrComponents>
        ): Fir2IrResult {
            val moduleDescriptor = FirModuleDescriptor(session, kotlinBuiltIns)
            val components = Fir2IrComponentsStorage(
                session, scopeSession, symbolTable, irFactory, signatureComposer, fir2IrExtensions, generateSignatures
            )
            val converter = Fir2IrConverter(moduleDescriptor, components)

            components.converter = converter

            val classifierStorage = Fir2IrClassifierStorage(components, dependentComponents.map { it.classifierStorage })
            components.classifierStorage = classifierStorage
            components.delegatedMemberGenerator = DelegatedMemberGenerator(components)
            val declarationStorage = Fir2IrDeclarationStorage(components, moduleDescriptor, dependentComponents.map { it.declarationStorage })
            components.declarationStorage = declarationStorage
            components.visibilityConverter = visibilityConverter
            val typeConverter = Fir2IrTypeConverter(components)
            components.typeConverter = typeConverter
            val irBuiltIns = dependentComponents.lastOrNull()?.irBuiltIns ?: IrBuiltInsOverFir(
                components, languageVersionSettings, moduleDescriptor, irMangler,
                languageVersionSettings.getFlag(AnalysisFlags.builtInsFromSources) || kotlinBuiltIns !== DefaultBuiltIns.Instance
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
            val irProvider = FirIrProvider(components)
            components.irProviders = listOf(irProvider)

            fir2IrExtensions.registerDeclarations(symbolTable)

            val irModuleFragment = IrModuleFragmentImpl(moduleDescriptor, irBuiltIns)

            val allFirFiles = buildList {
                addAll(firFiles)
                addAll(session.createFilesWithGeneratedDeclarations())
            }

            converter.runSourcesConversion(
                allFirFiles, irModuleFragment, irGenerationExtensions, fir2irVisitor, fir2IrExtensions
            )

            return Fir2IrResult(irModuleFragment, components, moduleDescriptor)
        }
    }
}

private class WrappedDescriptorSignatureComposer(
    private val delegate: IdSignatureComposer,
    private val firComposer: Fir2IrSignatureComposer
) : IdSignatureComposer by delegate {
    override fun withFileSignature(fileSignature: IdSignature.FileSignature, body: () -> Unit) {
        firComposer.withFileSignature(fileSignature) {
            delegate.withFileSignature(fileSignature, body)
        }
    }
}

private class DescriptorSignatureComposerStub : IdSignatureComposer {
    override fun composeSignature(descriptor: DeclarationDescriptor): IdSignature? {
        return null
    }

    override fun composeEnumEntrySignature(descriptor: ClassDescriptor): IdSignature? {
        return null
    }

    override fun composeFieldSignature(descriptor: PropertyDescriptor): IdSignature? {
        return null
    }

    override fun composeAnonInitSignature(descriptor: ClassDescriptor): IdSignature? {
        return null
    }

    override fun withFileSignature(fileSignature: IdSignature.FileSignature, body: () -> Unit) {
        body()
    }
}
