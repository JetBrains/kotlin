/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.evaluate.evaluateConstants
import org.jetbrains.kotlin.fir.backend.generators.AnnotationGenerator
import org.jetbrains.kotlin.fir.backend.generators.CallAndReferenceGenerator
import org.jetbrains.kotlin.fir.backend.generators.DataClassMembersGenerator
import org.jetbrains.kotlin.fir.backend.generators.FakeOverrideGenerator
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.signaturer.FirMangler
import org.jetbrains.kotlin.fir.symbols.FirBuiltinSymbols
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.descriptors.WrappedDeclarationDescriptor
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.psi2ir.generators.GeneratorExtensions

class Fir2IrConverter(
    private val moduleDescriptor: FirModuleDescriptor,
    private val sourceManager: PsiSourceManager,
    private val components: Fir2IrComponents
) : Fir2IrComponents by components {

    fun processLocalClassAndNestedClasses(regularClass: FirRegularClass, parent: IrDeclarationParent) {
        val irClass = registerClassAndNestedClasses(regularClass, parent)
        processClassAndNestedClassHeaders(regularClass)
        processClassMembers(regularClass, irClass)
    }

    fun processRegisteredLocalClassAndNestedClasses(regularClass: FirRegularClass, irClass: IrClass) {
        regularClass.declarations.forEach {
            if (it is FirRegularClass) {
                registerClassAndNestedClasses(it, irClass)
            }
        }
        processClassAndNestedClassHeaders(regularClass)
        processClassMembers(regularClass, irClass)
    }

    fun registerFileAndClasses(file: FirFile): IrFile {
        val irFile = IrFileImpl(
            sourceManager.getOrCreateFileEntry(file.psi as KtFile),
            moduleDescriptor.getPackage(file.packageFqName).fragments.first()
        )
        declarationStorage.registerFile(file, irFile)
        file.declarations.forEach {
            if (it is FirRegularClass) {
                registerClassAndNestedClasses(it, irFile)
            }
        }
        return irFile
    }

    fun processClassHeaders(file: FirFile) {
        file.declarations.forEach {
            when (it) {
                is FirRegularClass -> processClassAndNestedClassHeaders(it)
                is FirTypeAlias -> classifierStorage.registerTypeAlias(it, declarationStorage.getIrFile(file))
            }
        }
    }

    fun processFileAndClassMembers(file: FirFile) {
        val irFile = declarationStorage.getIrFile(file)
        for (declaration in file.declarations) {
            val irDeclaration = processMemberDeclaration(declaration, null, irFile) ?: continue
            irFile.declarations += irDeclaration
        }
    }

    fun processAnonymousObjectMembers(
        anonymousObject: FirAnonymousObject,
        irClass: IrClass = classifierStorage.getCachedIrClass(anonymousObject)!!
    ): IrClass {
        anonymousObject.getPrimaryConstructorIfAny()?.let {
            irClass.declarations += declarationStorage.createIrConstructor(
                it, irClass, isLocal = true
            )
        }
        val processedCallableNames = mutableSetOf<Name>()
        for (declaration in sortBySynthetic(anonymousObject.declarations)) {
            if (declaration is FirRegularClass) {
                registerClassAndNestedClasses(declaration, irClass)
                processClassAndNestedClassHeaders(declaration)
            }
            val irDeclaration = processMemberDeclaration(declaration, anonymousObject, irClass) ?: continue
            when (declaration) {
                is FirSimpleFunction -> processedCallableNames += declaration.name
                is FirProperty -> processedCallableNames += declaration.name
            }
            irClass.declarations += irDeclaration
        }
        // Add delegated members *before* fake override generations.
        // Otherwise, fake overrides for delegated members, which are redundant, will be added.
        val realDeclarations = delegatedMembers(irClass) + anonymousObject.declarations
        with(fakeOverrideGenerator) {
            irClass.addFakeOverrides(anonymousObject, realDeclarations)
        }

        return irClass
    }

    private fun processClassMembers(
        regularClass: FirRegularClass,
        irClass: IrClass = classifierStorage.getCachedIrClass(regularClass)!!
    ): IrClass {
        val irConstructor = regularClass.getPrimaryConstructorIfAny()?.let {
            declarationStorage.createIrConstructor(it, irClass, isLocal = regularClass.isLocal)
        }
        if (irConstructor != null) {
            irClass.declarations += irConstructor
        }
        val allDeclarations = regularClass.declarations.toMutableList()
        for (declaration in sortBySynthetic(regularClass.declarations)) {
            val irDeclaration = processMemberDeclaration(declaration, regularClass, irClass) ?: continue
            irClass.declarations += irDeclaration
        }
        // Add delegated members *before* fake override generations.
        // Otherwise, fake overrides for delegated members, which are redundant, will be added.
        allDeclarations += delegatedMembers(irClass)
        // Add synthetic members *before* fake override generations.
        // Otherwise, redundant members, e.g., synthetic toString _and_ fake override toString, will be added.
        if (irConstructor != null && (irClass.isInline || irClass.isData)) {
            declarationStorage.enterScope(irConstructor)
            val dataClassMembersGenerator = DataClassMembersGenerator(components)
            if (irClass.isInline) {
                allDeclarations += dataClassMembersGenerator.generateInlineClassMembers(regularClass, irClass)
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
    private fun sortBySynthetic(declarations: List<FirDeclaration>): Iterable<FirDeclaration> {
        return declarations.sortedBy { it.isSynthetic }
    }

    private fun registerClassAndNestedClasses(regularClass: FirRegularClass, parent: IrDeclarationParent): IrClass {
        val irClass = classifierStorage.registerIrClass(regularClass, parent)
        regularClass.declarations.forEach {
            if (it is FirRegularClass) {
                registerClassAndNestedClasses(it, irClass)
            }
        }
        return irClass
    }

    private fun processClassAndNestedClassHeaders(regularClass: FirRegularClass) {
        classifierStorage.processClassHeader(regularClass)
        regularClass.declarations.forEach {
            if (it is FirRegularClass) {
                processClassAndNestedClassHeaders(it)
            }
        }
    }

    private fun processMemberDeclaration(
        declaration: FirDeclaration,
        containingClass: FirClass<*>?,
        parent: IrDeclarationParent
    ): IrDeclaration? {
        val isLocal = containingClass != null &&
                (containingClass !is FirRegularClass || containingClass.isLocal)
        return when (declaration) {
            is FirRegularClass -> {
                processClassMembers(declaration)
            }
            is FirSimpleFunction -> {
                declarationStorage.createIrFunction(
                    declaration, parent, isLocal = isLocal
                )
            }
            is FirProperty -> {
                declarationStorage.createIrProperty(
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
                declarationStorage.createIrConstructor(
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
        fun createModuleFragment(
            session: FirSession,
            scopeSession: ScopeSession,
            firFiles: List<FirFile>,
            languageVersionSettings: LanguageVersionSettings,
            signaturer: IdSignatureComposer,
            generatorExtensions: GeneratorExtensions,
            mangler: FirMangler,
            irFactory: IrFactory,
            visibilityConverter: Fir2IrVisibilityConverter
        ): Fir2IrResult {
            val moduleDescriptor = FirModuleDescriptor(session)
            val symbolTable = SymbolTable(signaturer, irFactory)
            val constantValueGenerator = ConstantValueGenerator(moduleDescriptor, symbolTable)
            val typeTranslator = TypeTranslator(
                symbolTable,
                languageVersionSettings,
                moduleDescriptor.builtIns,
                extensions = generatorExtensions
            )
            constantValueGenerator.typeTranslator = typeTranslator
            typeTranslator.constantValueGenerator = constantValueGenerator
            val irBuiltIns = IrBuiltIns(moduleDescriptor.builtIns, typeTranslator, symbolTable)
            FirBuiltinSymbols(irBuiltIns, moduleDescriptor.builtIns, symbolTable)
            val sourceManager = PsiSourceManager()
            val components = Fir2IrComponentsStorage(session, scopeSession, symbolTable, irBuiltIns, irFactory, mangler)
            val conversionScope = Fir2IrConversionScope()
            val classifierStorage = Fir2IrClassifierStorage(components)
            val declarationStorage = Fir2IrDeclarationStorage(components, moduleDescriptor)
            val typeConverter = Fir2IrTypeConverter(components)
            val builtIns = Fir2IrBuiltIns(components, session)
            components.declarationStorage = declarationStorage
            components.classifierStorage = classifierStorage
            components.typeConverter = typeConverter
            components.visibilityConverter = visibilityConverter
            components.builtIns = builtIns
            val irFiles = mutableListOf<IrFile>()

            val converter = Fir2IrConverter(moduleDescriptor, sourceManager, components)
            for (firFile in firFiles) {
                irFiles += converter.registerFileAndClasses(firFile)
            }
            val irModuleFragment = IrModuleFragmentImpl(moduleDescriptor, irBuiltIns, irFiles)
            val irProviders =
                generateTypicalIrProviderList(irModuleFragment.descriptor, irBuiltIns, symbolTable, extensions = generatorExtensions)
            val externalDependenciesGenerator = ExternalDependenciesGenerator(
                symbolTable,
                irProviders,
                languageVersionSettings
            )
            // Necessary call to generate built-in IR classes
            externalDependenciesGenerator.generateUnboundSymbolsAsDependencies()
            classifierStorage.preCacheBuiltinClasses()
            val fakeOverrideGenerator = FakeOverrideGenerator(
                session, scopeSession, classifierStorage, declarationStorage, conversionScope
            )
            components.fakeOverrideGenerator = fakeOverrideGenerator
            val fir2irVisitor = Fir2IrVisitor(converter, components, conversionScope)
            val callGenerator = CallAndReferenceGenerator(components, fir2irVisitor, conversionScope)
            components.callGenerator = callGenerator
            declarationStorage.annotationGenerator = AnnotationGenerator(components)
            for (firFile in firFiles) {
                converter.processClassHeaders(firFile)
            }
            for (firFile in firFiles) {
                converter.processFileAndClassMembers(firFile)
            }

            for (firFile in firFiles) {
                val irFile = firFile.accept(fir2irVisitor, null) as IrFile
                val fileEntry = sourceManager.getOrCreateFileEntry(firFile.psi as KtFile)
                sourceManager.putFileEntry(irFile, fileEntry)
            }

            externalDependenciesGenerator.generateUnboundSymbolsAsDependencies()
            val stubGenerator = irProviders.filterIsInstance<DeclarationStubGenerator>().first()
            for (descriptor in symbolTable.wrappedTopLevelCallableDescriptors()) {
                val parentClass = stubGenerator.generateOrGetFacadeClass(descriptor as WrappedDeclarationDescriptor<*>)
                descriptor.owner.parent = parentClass ?: continue
            }

            evaluateConstants(irModuleFragment)

            return Fir2IrResult(irModuleFragment, symbolTable, sourceManager, components)
        }
    }
}
