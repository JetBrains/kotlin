/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtPsiSourceFileLinesMapping
import org.jetbrains.kotlin.KtSourceFileLinesMappingFromLineStartOffsets
import org.jetbrains.kotlin.backend.common.CommonBackendErrors
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.backend.generators.DataClassMembersGenerator
import org.jetbrains.kotlin.fir.backend.generators.FirBasedFakeOverrideGenerator
import org.jetbrains.kotlin.fir.backend.generators.addDeclarationToParent
import org.jetbrains.kotlin.fir.backend.generators.setParent
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.correspondingValueParameterFromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.declarations.utils.isSynthetic
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.extensions.declarationGenerators
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.generatedMembers
import org.jetbrains.kotlin.fir.extensions.generatedNestedClassifiers
import org.jetbrains.kotlin.fir.java.javaElementFinder
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.fir.references.toResolvedValueParameterSymbol
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.symbols.lazyDeclarationResolver
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.PsiIrFileEntry
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterConfiguration
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterEnvironment
import org.jetbrains.kotlin.ir.interpreter.checker.EvaluationMode
import org.jetbrains.kotlin.ir.interpreter.transformer.transformConst
import org.jetbrains.kotlin.ir.overrides.IrFakeOverrideBuilder
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorPublicSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionPublicSymbolImpl
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile

class Fir2IrConverter(
    private val moduleDescriptor: FirModuleDescriptor,
    private val components: Fir2IrComponents,
    private val conversionScope: Fir2IrConversionScope,
) : Fir2IrComponents by components {

    private val generatorExtensions = session.extensionService.declarationGenerators

    @FirBasedFakeOverrideGenerator
    private var wereSourcesFakeOverridesBound = false
    private val postponedDeclarationsForFakeOverridesBinding = mutableListOf<IrDeclaration>()

    private fun runSourcesConversion(
        allFirFiles: List<FirFile>,
        irModuleFragment: IrModuleFragmentImpl,
        fir2irVisitor: Fir2IrVisitor
    ) {
        for (firFile in allFirFiles) {
            registerFileAndClasses(firFile, irModuleFragment)
        }
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
        @OptIn(FirBasedFakeOverrideGenerator::class) // checked for useIrFakeOverrideBuilder
        if (!configuration.useIrFakeOverrideBuilder) {
            for (firFile in allFirFiles) {
                bindFakeOverridesInFile(firFile)
            }

            wereSourcesFakeOverridesBound = true
            fakeOverrideGenerator.bindOverriddenSymbols(postponedDeclarationsForFakeOverridesBinding)
            postponedDeclarationsForFakeOverridesBinding.clear()
        } else {
            require(postponedDeclarationsForFakeOverridesBinding.isEmpty())
        }

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

        if (
            !configuration.useIrFakeOverrideBuilder &&
            components.session.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)
        ) {
            // See the comment to generateUnboundFakeOverrides function itself
            @OptIn(LeakedDeclarationCaches::class)
            declarationStorage.generateUnboundFakeOverrides()
        }

        if (configuration.allowNonCachedDeclarations) {
            // See the comment to fillUnboundSymbols function itself
            @OptIn(LeakedDeclarationCaches::class)
            declarationStorage.fillUnboundSymbols()
        }
    }

    @FirBasedFakeOverrideGenerator
    fun bindFakeOverridesOrPostpone(declarations: List<IrDeclaration>) {
        // Do not run binding for lazy classes until all sources declarations are processed
        if (wereSourcesFakeOverridesBound) {
            fakeOverrideGenerator.bindOverriddenSymbols(declarations)
        } else {
            postponedDeclarationsForFakeOverridesBinding += declarations
        }
    }

    fun processLocalClassAndNestedClassesOnTheFly(klass: FirClass, parent: IrDeclarationParent): IrClass {
        val irClass = registerClassAndNestedClasses(klass, parent)
        conversionScope.withContainingFirClass(klass) {
            processClassAndNestedClassHeaders(klass)
        }
        return irClass
    }

    fun processLocalClassAndNestedClasses(klass: FirClass, parent: IrDeclarationParent): IrClass {
        val irClass = registerClassAndNestedClasses(klass, parent)
        conversionScope.withContainingFirClass(klass) {
            processClassAndNestedClassHeaders(klass)
            processClassMembers(klass, irClass)
            bindFakeOverridesInClass(irClass)
        }
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
            is FirDeclarationOrigin.Synthetic -> NaiveSourceBasedFileEntryImpl(file.name)
            else -> error("Unsupported file origin: ${file.origin}")
        }
        val irFile = IrFileImpl(
            fileEntry,
            moduleDescriptor.getPackage(file.packageFqName).fragments.first(),
            moduleFragment
        )
        declarationStorage.registerFile(file, irFile)
        for (declaration in file.declarations) {
            when (declaration) {
                is FirRegularClass -> registerClassAndNestedClasses(declaration, irFile)
                is FirCodeFragment -> classifierStorage.createAndCacheCodeFragmentClass(declaration, irFile)
                else -> {}
            }
        }
        moduleFragment.files += irFile
    }

    private fun processClassHeaders(file: FirFile) {
        val irFile = declarationStorage.getIrFile(file)
        file.declarations.forEach {
            when (it) {
                is FirRegularClass -> processClassAndNestedClassHeaders(it)
                is FirTypeAlias -> {
                    classifierStorage.createAndCacheIrTypeAlias(it, irFile)
                }
                else -> {}
            }
        }
        /*
         * This is needed to preserve the source order of declarations in the file
         * IrFile should contain declarations in the source order, but creating of IrClass automatically adds created class to the list
         *   of file declaration. And in this step we skip all callables, so by default all classes will be declared before all callables
         * To fix this issue the list of declarations is cleared at this point, and later it will be filled again in `processClassMembers`
         *
         * `irFile` is definitely not a lazy class
         */
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        irFile.declarations.clear()
    }

    private fun processFileAndClassMembers(file: FirFile) {
        val irFile = declarationStorage.getIrFile(file)
        for (declaration in file.declarations) {
            processMemberDeclaration(declaration, containingClass = null, irFile, delegateFieldToPropertyMap = null)
        }
    }

    fun processAnonymousObjectHeaders(
        anonymousObject: FirAnonymousObject,
        irClass: IrClass,
    ) {
        registerNestedClasses(anonymousObject, irClass)
        processNestedClassHeaders(anonymousObject)
        /*
         * This is needed to preserve the source order of declarations in the class
         * IrClass should contain declarations in the source order, but creating of nested IrClass automatically adds created class to the list
         *   of class declaration. And in this step we skip all callables, so by default all classes will be declared before all callables
         * To fix this issue the list of declarations is cleared at this point, and later it will be filled again in `processClassMembers`
         *
         * `irClass` is a source class and definitely is not a lazy class
         */
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        irClass.declarations.clear()
    }

    internal fun processClassMembers(klass: FirClass, irClass: IrClass): IrClass {
        val allDeclarations = mutableListOf<FirDeclaration>().apply {
            addAll(klass.declarations)
            if (klass is FirRegularClass && generatorExtensions.isNotEmpty()) {
                addAll(klass.generatedMembers(session))
                addAll(klass.generatedNestedClassifiers(session))
            }
        }

        // `irClass` is a source class and definitely is not a lazy class
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        irClass.declarations.addAll(classifierStorage.getFieldsWithContextReceiversForClass(irClass, klass))

        val irConstructor = klass.primaryConstructorIfAny(session)?.let {
            declarationStorage.createAndCacheIrConstructor(it.fir, { irClass }, isLocal = klass.isLocal)
        }

        val delegateFieldToPropertyMap = MultiMap<FirProperty, FirField>()

        // At least on enum entry creation we may need a default constructor, so ctors should be converted first
        for (declaration in syntheticPropertiesLast(allDeclarations)) {
            processMemberDeclaration(declaration, klass, irClass, delegateFieldToPropertyMap)
        }
        // Add delegated members *before* fake override generations.
        // Otherwise, fake overrides for delegated members, which are redundant, will be added.
        allDeclarations += delegatedMembers(irClass)
        // Add synthetic members *before* fake override generations.
        // Otherwise, redundant members, e.g., synthetic toString _and_ fake override toString, will be added.
        if (klass is FirRegularClass && irConstructor != null && (irClass.isValue || irClass.isData)) {
            declarationStorage.enterScope(irConstructor.symbol)
            val dataClassMembersGenerator = DataClassMembersGenerator(components)
            if (irClass.isSingleFieldValueClass) {
                allDeclarations += dataClassMembersGenerator.generateSingleFieldValueClassMembers(klass, irClass)
            }
            if (irClass.isMultiFieldValueClass) {
                allDeclarations += dataClassMembersGenerator.generateMultiFieldValueClassMembers(klass, irClass)
            }
            if (irClass.isData) {
                allDeclarations += dataClassMembersGenerator.generateDataClassMembers(klass, irClass)
            }
            declarationStorage.leaveScope(irConstructor.symbol)
        }

        if (!configuration.useIrFakeOverrideBuilder) {
            @OptIn(FirBasedFakeOverrideGenerator::class) // checked for useIrFakeOverrideBuilder
            fakeOverrideGenerator.computeFakeOverrides(klass, irClass, allDeclarations)
        }

        return irClass
    }

    private fun processCodeFragmentMembers(codeFragment: FirCodeFragment, irClass: IrClass): IrClass {
        val conversionData = codeFragment.conversionData

        declarationStorage.enterScope(irClass.symbol)

        val signature = irClass.symbol.signature!!

        symbolTable.declareConstructor(signature, { IrConstructorPublicSymbolImpl(signature) }) { irSymbol ->
            irFactory.createConstructor(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                IrDeclarationOrigin.DEFINED,
                Name.special("<init>"),
                irClass.visibility,
                isInline = false,
                isExpect = false,
                irClass.defaultType,
                irSymbol,
                isExternal = false,
                isPrimary = true
            ).apply {
                setParent(irClass)
                addDeclarationToParent(this, irClass)
                val firAnyConstructor = session.builtinTypes.anyType.toRegularClassSymbol(session)!!.fir.primaryConstructorIfAny(session)!!
                val irAnyConstructor = declarationStorage.getIrConstructorSymbol(firAnyConstructor)
                body = irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET).apply {
                    statements += IrDelegatingConstructorCallImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        irBuiltIns.unitType,
                        irAnyConstructor,
                        typeArgumentsCount = 0,
                        valueArgumentsCount = 0
                    )
                }
            }
        }

        symbolTable.declareSimpleFunction(signature, { IrSimpleFunctionPublicSymbolImpl(signature) }) { irSymbol ->
            val lastStatement = codeFragment.block.statements.lastOrNull()
            val returnType = (lastStatement as? FirExpression)?.resolvedType?.toIrType(typeConverter) ?: irBuiltIns.unitType

            irFactory.createSimpleFunction(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                IrDeclarationOrigin.DEFINED,
                conversionData.methodName,
                DescriptorVisibilities.PUBLIC,
                isInline = false,
                isExpect = false,
                returnType,
                Modality.FINAL,
                irSymbol,
                isTailrec = false,
                isSuspend = false,
                isOperator = false,
                isInfix = false,
                isExternal = false,
                containerSource = null,
                isFakeOverride = false
            ).apply fragmentFunction@{
                setParent(irClass)
                addDeclarationToParent(this, irClass)
                valueParameters = conversionData.injectedValues.mapIndexed { index, injectedValue ->
                    val isMutated = injectedValue.isMutated

                    irFactory.createValueParameter(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        if (isMutated) IrDeclarationOrigin.SHARED_VARIABLE_IN_EVALUATOR_FRAGMENT else IrDeclarationOrigin.DEFINED,
                        Name.identifier("p$index"),
                        injectedValue.typeRef.toIrType(typeConverter),
                        isAssignable = isMutated,
                        injectedValue.irParameterSymbol,
                        index,
                        varargElementType = null,
                        isCrossinline = false,
                        isNoinline = false,
                        isHidden = false
                    ).apply {
                        parent = this@fragmentFunction
                    }
                }
            }
        }

        declarationStorage.leaveScope(irClass.symbol)
        return irClass
    }

    @FirBasedFakeOverrideGenerator
    private fun bindFakeOverridesInFile(file: FirFile) {
        val irFile = declarationStorage.getIrFile(file)
        // `irFile` definitely is not a lazy class
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        for (irDeclaration in irFile.declarations) {
            if (irDeclaration is IrClass) {
                bindFakeOverridesInClass(irDeclaration)
            }
        }
    }

    // `irClass` is a source class and definitely is not a lazy class
    // checked for useIrFakeOverrideBuilder
    @OptIn(UnsafeDuringIrConstructionAPI::class, FirBasedFakeOverrideGenerator::class)
    fun bindFakeOverridesInClass(klass: IrClass) {
        if (configuration.useIrFakeOverrideBuilder) return
        require(klass !is Fir2IrLazyClass)
        fakeOverrideGenerator.bindOverriddenSymbols(klass.declarations)
        delegatedMemberGenerator.bindDelegatedMembersOverriddenSymbols(klass)
        for (irDeclaration in klass.declarations) {
            if (irDeclaration is IrClass) {
                bindFakeOverridesInClass(irDeclaration)
            }
        }
    }

    // `irClass` is a source class and definitely is not a lazy class
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun delegatedMembers(irClass: IrClass): List<FirDeclaration> {
        return irClass.declarations.filter {
            it.origin == IrDeclarationOrigin.DELEGATED_MEMBER
        }.mapNotNull {
            components.declarationStorage.originalDeclarationForDelegated(it)
        }
    }

    // Sort declarations so that all non-synthetic declarations and `synthetic class delegation fields` are before other synthetic ones.
    // This is needed because converting synthetic fields for implementation delegation needs to know
    // existing declarations in the class to avoid adding redundant delegated members.
    private fun syntheticPropertiesLast(declarations: Iterable<FirDeclaration>): Iterable<FirDeclaration> {
        return declarations.sortedBy {
            when {
                !it.isSynthetic -> false
                it.source?.kind is KtFakeSourceElementKind.ClassDelegationField -> false
                else -> true
            }
        }
    }

    private fun registerClassAndNestedClasses(klass: FirClass, parent: IrDeclarationParent): IrClass {
        // Local classes might be referenced before they declared (see usages of Fir2IrClassifierStorage.createLocalIrClassOnTheFly)
        // So, we only need to set its parent properly
        val irClass =
            classifierStorage.getCachedIrClass(klass)?.apply {
                this.parent = parent
            } ?: when (klass) {
                is FirRegularClass -> classifierStorage.createAndCacheIrClass(klass, parent)
                is FirAnonymousObject -> classifierStorage.createAndCacheAnonymousObject(klass, irParent = parent)
            }
        registerNestedClasses(klass, irClass)
        return irClass
    }

    private fun registerNestedClasses(klass: FirClass, irClass: IrClass) {
        klass.declarations.forEach {
            if (it is FirRegularClass) {
                registerClassAndNestedClasses(it, irClass)
            }
        }
        if (klass is FirRegularClass && generatorExtensions.isNotEmpty()) {
            klass.generatedNestedClassifiers(session).forEach {
                if (it is FirRegularClass) {
                    registerClassAndNestedClasses(it, irClass)
                }
            }
        }
    }

    private fun processClassAndNestedClassHeaders(klass: FirClass) {
        classifiersGenerator.processClassHeader(klass)
        processNestedClassHeaders(klass)
        val irClass = classifierStorage.getCachedIrClass(klass)!!
        /*
         * This is needed to preserve the source order of declarations in the class
         * IrClass should contain declarations in the source order, but creating of nested IrClass automatically adds created class to the list
         *   of class declaration. And in this step we skip all callables, so by default all classes will be declared before all callables
         * To fix this issue the list of declarations is cleared at this point, and later it will be filled again in `processFileAndClassMembers`
         *
         * `irClass` is a source class and definitely is not a lazy class
         */
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        irClass.declarations.clear()
    }

    private fun processNestedClassHeaders(klass: FirClass) {
        klass.declarations.forEach {
            if (it is FirRegularClass) {
                processClassAndNestedClassHeaders(it)
            }
        }
        if (klass is FirRegularClass && generatorExtensions.isNotEmpty()) {
            klass.generatedNestedClassifiers(session).forEach {
                if (it is FirRegularClass) {
                    processClassAndNestedClassHeaders(it)
                }
            }
        }
    }

    /**
     * This function creates IR declarations for callable members without filling their body
     *
     * @param delegateFieldToPropertyMap is needed to avoid problems with delegation to properties from primary constructor.
     * The thing is that FirFields for delegates are declared before properties from the primary constructor, but in IR we don't
     *   create separate IrField for such fields and reuse the backing field of corresponding property.
     *   So, this map is used to postpone generation of delegated members until IR for corresponding property will be created
     */
    private fun processMemberDeclaration(
        declaration: FirDeclaration,
        containingClass: FirClass?,
        parent: IrDeclarationParent,
        delegateFieldToPropertyMap: MultiMap<FirProperty, FirField>?
    ) {
        /*
         * This function is needed to preserve the source order of declaration in file
         * see the comment in [processClassHeaders] function
         *
         * `irClass` is a source class and definitely is not a lazy class
         */
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        fun addDeclarationToParentIfNeeded(irDeclaration: IrDeclaration) {
            when (parent) {
                is IrFile -> parent.declarations += irDeclaration
                is IrClass -> parent.declarations += irDeclaration
            }
        }

        val isInLocalClass = containingClass != null && (containingClass !is FirRegularClass || containingClass.isLocal)
        when (declaration) {
            is FirRegularClass -> {
                val irClass = classifierStorage.getCachedIrClass(declaration)!!
                addDeclarationToParentIfNeeded(irClass)
                processClassMembers(declaration, irClass)
            }
            is FirScript -> {
                require(parent is IrFile)
                val irScript = declarationStorage.createIrScript(declaration)
                addDeclarationToParentIfNeeded(irScript)
                declarationStorage.withScope(irScript.symbol) {
                    irScript.parent = parent
                    for (scriptDeclaration in declaration.declarations) {
                        when (scriptDeclaration) {
                            is FirRegularClass -> {
                                registerClassAndNestedClasses(scriptDeclaration, irScript)
                                processClassAndNestedClassHeaders(scriptDeclaration)
                            }
                            is FirTypeAlias -> classifierStorage.createAndCacheIrTypeAlias(scriptDeclaration, irScript)
                            else -> {}
                        }
                    }
                    for (scriptDeclaration in declaration.declarations) {
                        if (scriptDeclaration !is FirAnonymousInitializer) {
                            processMemberDeclaration(scriptDeclaration, containingClass = null, irScript, delegateFieldToPropertyMap = null)
                        }
                    }
                }
            }
            is FirSimpleFunction -> {
                declarationStorage.createAndCacheIrFunction(declaration, parent, isLocal = isInLocalClass)
            }
            is FirProperty -> {
                if (
                    containingClass == null ||
                    !declaration.isEnumEntries(containingClass) ||
                    session.languageVersionSettings.supportsFeature(LanguageFeature.EnumEntries)
                ) {
                    // Note: we have to do it, because backend without the feature
                    // cannot process Enum.entries properly
                    val irProperty = declarationStorage.createAndCacheIrProperty(declaration, parent)
                    delegateFieldToPropertyMap?.remove(declaration)?.let { delegateFields ->
                        val backingField = irProperty.backingField!!
                        for (delegateField in delegateFields) {
                            declarationStorage.recordDelegateFieldMappedToBackingField(delegateField, backingField.symbol)
                            delegatedMemberGenerator.generateWithBodiesIfNeeded(
                                firField = delegateField,
                                irField = backingField,
                                containingClass!!,
                                parent as IrClass
                            )
                        }
                    }
                }
            }
            is FirField -> {
                if (!declaration.isSynthetic) {
                    error("Unexpected non-synthetic field: ${declaration::class}")
                }
                requireNotNull(containingClass)
                requireNotNull(delegateFieldToPropertyMap)
                require(parent is IrClass)
                val correspondingClassProperty = declaration.findCorrespondingDelegateProperty(containingClass)
                if (correspondingClassProperty == null || correspondingClassProperty.isVar) {
                    val irField = declarationStorage.createDelegateIrField(declaration, parent)
                    delegatedMemberGenerator.generateWithBodiesIfNeeded(declaration, irField, containingClass, parent)
                } else {
                    delegateFieldToPropertyMap.putValue(correspondingClassProperty, declaration)
                }
            }
            is FirConstructor -> if (!declaration.isPrimary) {
                // the primary constructor was already created in `processClassMembers` function
                declarationStorage.createAndCacheIrConstructor(declaration, { parent as IrClass }, isLocal = isInLocalClass)
            }
            is FirEnumEntry -> {
                classifierStorage.getOrCreateIrEnumEntry(declaration, parent as IrClass)
            }
            is FirAnonymousInitializer -> {
                declarationStorage.createIrAnonymousInitializer(declaration, parent as IrClass)
            }
            is FirTypeAlias -> {
                classifierStorage.getCachedTypeAlias(declaration)?.let { irTypeAlias ->
                    // type alias may be local with error suppression, so it might be missing from classifier storage
                    addDeclarationToParentIfNeeded(irTypeAlias)
                }
            }
            is FirCodeFragment -> {
                val codeFragmentClass = classifierStorage.getCachedIrCodeFragment(declaration)!!
                processCodeFragmentMembers(declaration, codeFragmentClass)
                addDeclarationToParentIfNeeded(codeFragmentClass)
            }
            else -> {
                error("Unexpected member: ${declaration::class}")
            }
        }
    }

    private fun FirField.findCorrespondingDelegateProperty(owner: FirClass): FirProperty? {
        val initializer = this.initializer
        if (initializer !is FirQualifiedAccessExpression) return null
        if (initializer.explicitReceiver != null) return null
        val resolvedSymbol = initializer.calleeReference.toResolvedValueParameterSymbol() ?: return null
        return owner.declarations.filterIsInstance<FirProperty>().find {
            it.correspondingValueParameterFromPrimaryConstructor == resolvedSymbol
        }
    }

    companion object {
        // TODO: move to compiler/fir/entrypoint/src/org/jetbrains/kotlin/fir/pipeline/convertToIr.kt (KT-64201)
        fun evaluateConstants(irModuleFragment: IrModuleFragment, components: Fir2IrComponents) {
            val fir2IrConfiguration = components.configuration
            val firModuleDescriptor = irModuleFragment.descriptor as? FirModuleDescriptor
            val targetPlatform = firModuleDescriptor?.platform
            val languageVersionSettings = firModuleDescriptor?.session?.languageVersionSettings ?: return
            val intrinsicConstEvaluation = languageVersionSettings.supportsFeature(LanguageFeature.IntrinsicConstEvaluation)

            val configuration = IrInterpreterConfiguration(
                platform = targetPlatform,
                printOnlyExceptionMessage = true,
            )

            val interpreter = IrInterpreter(IrInterpreterEnvironment(irModuleFragment.irBuiltins, configuration))
            val mode = if (intrinsicConstEvaluation) EvaluationMode.ONLY_INTRINSIC_CONST else EvaluationMode.ONLY_BUILTINS

            components.session.javaElementFinder?.propertyEvaluator = { it.evaluate(components, interpreter, mode) }

            val ktDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
                fir2IrConfiguration.diagnosticReporter, languageVersionSettings
            )
            irModuleFragment.files.forEach {
                it.transformConst(
                    it,
                    interpreter,
                    mode,
                    fir2IrConfiguration.evaluatedConstTracker,
                    fir2IrConfiguration.inlineConstTracker,
                    onError = { irFile, element, error ->
                        // We are using exactly this overload of `at` to eliminate differences between PSI and LightTree render
                        ktDiagnosticReporter.at(element.sourceElement(), element, irFile)
                            .report(CommonBackendErrors.EVALUATION_ERROR, error.description)
                    }
                )
            }
        }

        private fun FirProperty.evaluate(components: Fir2IrComponents, interpreter: IrInterpreter, mode: EvaluationMode): String? {
            @OptIn(UnsafeDuringIrConstructionAPI::class)
            val irProperty = components.declarationStorage.getCachedIrPropertySymbol(
                property = this, fakeOverrideOwnerLookupTag = null
            )?.owner ?: return null

            fun IrProperty.tryToGetConst(): IrConst<*>? = (backingField?.initializer?.expression as? IrConst<*>)
            fun IrConst<*>.asString(): String {
                return when (val constVal = value) {
                    is Char -> constVal.code.toString()
                    is String -> "\"$constVal\""
                    else -> constVal.toString()
                }
            }
            irProperty.tryToGetConst()?.let { return it.asString() }

            val irFile = irProperty.fileOrNull ?: return null
            // Note: can't evaluate all expressions in given file, because we can accidentally get recursive processing and
            // second call of `Fir2IrLazyField.initializer` will return null
            val evaluated = irProperty.transformConst(
                irFile, interpreter, mode,
                evaluatedConstTracker = components.configuration.evaluatedConstTracker,
                inlineConstTracker = components.configuration.inlineConstTracker,
            )

            return (evaluated as? IrProperty)?.tryToGetConst()?.asString()
        }

        // TODO: drop this function in favor of using [IrModuleDescriptor::shouldSeeInternalsOf] in FakeOverrideBuilder KT-61384
        private fun friendModulesMap(session: FirSession): Map<String, List<String>> {
            fun FirModuleData.friendsMapName() = name.asStringStripSpecialMarkers()
            fun FirModuleData.collectDependsOnRecursive(set: MutableSet<FirModuleData>) {
                if (!set.add(this)) return
                for (dep in dependsOnDependencies) {
                    dep.collectDependsOnRecursive(set)
                }
            }
            val moduleData = session.moduleData
            val dependsOnTransitive = buildSet {
                moduleData.collectDependsOnRecursive(this)
            }
            val friendNames = (moduleData.friendDependencies + dependsOnTransitive).map { it.friendsMapName() }
            return dependsOnTransitive.associate { it.friendsMapName() to friendNames }
        }

        fun createIrModuleFragment(
            session: FirSession,
            scopeSession: ScopeSession,
            firFiles: List<FirFile>,
            fir2IrExtensions: Fir2IrExtensions,
            fir2IrConfiguration: Fir2IrConfiguration,
            irMangler: KotlinMangler.IrMangler,
            irFactory: IrFactory,
            visibilityConverter: Fir2IrVisibilityConverter,
            specialSymbolProvider: Fir2IrSpecialSymbolProvider,
            kotlinBuiltIns: KotlinBuiltIns,
            commonMemberStorage: Fir2IrCommonMemberStorage,
            initializedIrBuiltIns: IrBuiltInsOverFir?,
            typeContextProvider: (IrBuiltIns) -> IrTypeSystemContext
        ): Fir2IrResult {
            session.lazyDeclarationResolver.disableLazyResolveContractChecks()
            val moduleDescriptor = FirModuleDescriptor.createSourceModuleDescriptor(session, kotlinBuiltIns)
            val components = Fir2IrComponentsStorage(
                session, scopeSession, irFactory, fir2IrExtensions, fir2IrConfiguration, visibilityConverter,
                { irBuiltins ->
                    IrFakeOverrideBuilder(
                        typeContextProvider(irBuiltins),
                        Fir2IrFakeOverrideStrategy(friendModulesMap(session), commonMemberStorage.symbolTable, irMangler),
                        fir2IrExtensions.externalOverridabilityConditions
                    )
                },
                moduleDescriptor, commonMemberStorage, irMangler, specialSymbolProvider, initializedIrBuiltIns
            )

            fir2IrExtensions.registerDeclarations(commonMemberStorage.symbolTable)

            val irModuleFragment = IrModuleFragmentImpl(moduleDescriptor, components.irBuiltIns)

            val allFirFiles = buildList {
                addAll(firFiles)
                val generatedFiles = session.createFilesWithGeneratedDeclarations()
                addAll(generatedFiles)
                generatedFiles.forEach { components.firProvider.recordFile(it) }
            }

            components.converter.runSourcesConversion(
                allFirFiles,
                irModuleFragment,
                components.fir2IrVisitor
            )

            commonMemberStorage.registerFirProvider(session.moduleData, components.firProvider)

            return Fir2IrResult(irModuleFragment, components, moduleDescriptor)
        }
    }
}
