/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtPsiSourceFileLinesMapping
import org.jetbrains.kotlin.KtSourceFileLinesMappingFromLineStartOffsets
import org.jetbrains.kotlin.backend.common.CommonBackendErrors
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.backend.generators.addDeclarationToParent
import org.jetbrains.kotlin.fir.backend.generators.setParent
import org.jetbrains.kotlin.fir.backend.utils.createFilesWithBuiltinsSyntheticDeclarationsIfNeeded
import org.jetbrains.kotlin.fir.backend.utils.createFilesWithGeneratedDeclarations
import org.jetbrains.kotlin.fir.backend.utils.unsubstitutedScope
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.extensions.declarationGenerators
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.generatedMembers
import org.jetbrains.kotlin.fir.extensions.generatedNestedClassifiers
import org.jetbrains.kotlin.fir.java.javaElementFinder
import org.jetbrains.kotlin.fir.references.toResolvedValueParameterSymbol
import org.jetbrains.kotlin.fir.scopes.processAllFunctions
import org.jetbrains.kotlin.fir.symbols.lazyDeclarationResolver
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.PsiIrFileEntry
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImplWithShape
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterConfiguration
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterEnvironment
import org.jetbrains.kotlin.ir.interpreter.checker.EvaluationMode
import org.jetbrains.kotlin.ir.interpreter.transformer.transformConst
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.sourceElement
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtFile

class Fir2IrConverter(
    private val moduleDescriptor: FirModuleDescriptor,
    private val c: Fir2IrComponents,
    private val conversionScope: Fir2IrConversionScope,
) : Fir2IrComponents by c {

    private val generatorExtensions = session.extensionService.declarationGenerators

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

        //   Do (3) for local classes encountered during (3)
        classifierStorage.processMembersOfClassesCreatedOnTheFly()

        //   4. Body processing
        //   If we encounter local class / anonymous object here, then we perform all (1)-(5) stages immediately
        for (firFile in allFirFiles) {
            withFileAnalysisExceptionWrapping(firFile) {
                firFile.accept(fir2irVisitor, null)
            }
        }

        if (configuration.allowNonCachedDeclarations) {
            // See the comment to fillUnboundSymbols function itself
            @OptIn(LeakedDeclarationCaches::class)
            declarationStorage.fillUnboundSymbols()
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
            if (session.languageVersionSettings.getFlag(JvmAnalysisFlags.expectBuiltinsAsPartOfStdlib)) {
                // For kotlin classes mapped to JDK classes, we must create IR for 'enhanced' functions
                // They might be queried as owners of overridden symbols
                if (JavaToKotlinClassMap.mapKotlinToJava(klass.classId.asSingleFqName().toUnsafe()) != null) {
                    klass.unsubstitutedScope(c).processAllFunctions {
                        // additional check to add IR declarations only in declaring class
                        if (it.origin == FirDeclarationOrigin.Enhancement && it.callableId.classId == klass.classId) {
                            add(it.fir)
                        }
                    }
                }
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
        return irClass
    }

    private fun processCodeFragmentMembers(codeFragment: FirCodeFragment, irClass: IrClass): IrClass {
        val conversionData = extensions.codeFragmentConversionData(codeFragment)

        declarationStorage.enterScope(irClass.symbol)

        IrConstructorSymbolImpl().let { irSymbol ->
            IrFactoryImpl.createConstructor(
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
                body = IrFactoryImpl.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET).apply {
                    statements += IrDelegatingConstructorCallImplWithShape(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        builtins.unitType,
                        irAnyConstructor,
                        typeArgumentsCount = 0,
                        valueArgumentsCount = 0,
                        contextParameterCount = 0,
                        hasDispatchReceiver = false,
                        hasExtensionReceiver = false,
                    )
                }
            }
        }

        IrSimpleFunctionSymbolImpl().let { irSymbol ->
            val lastStatement = codeFragment.block.statements.lastOrNull()
            val returnType = (lastStatement as? FirExpression)?.resolvedType?.toIrType(c) ?: builtins.unitType

            IrFactoryImpl.createSimpleFunction(
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

                    IrFactoryImpl.createValueParameter(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        if (isMutated) IrDeclarationOrigin.SHARED_VARIABLE_IN_EVALUATOR_FRAGMENT else IrDeclarationOrigin.DEFINED,
                        Name.identifier("p$index"),
                        injectedValue.typeRef.toIrType(typeConverter),
                        isAssignable = isMutated,
                        injectedValue.irParameterSymbol,
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

    // `irClass` is a source class and definitely is not a lazy class
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun delegatedMembers(irClass: IrClass): List<FirDeclaration> {
        return irClass.declarations.filter {
            it.origin == IrDeclarationOrigin.DELEGATED_MEMBER
        }.mapNotNull {
            c.declarationStorage.originalDeclarationForDelegated(it)
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
        val irClass = classifierStorage.getCachedIrLocalClass(klass)?.apply {
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
        val irClass = classifierStorage.getIrClass(klass)
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
                val irClass = classifierStorage.getIrClass(declaration)
                addDeclarationToParentIfNeeded(irClass)
                processClassMembers(declaration, irClass)
            }
            is FirScript -> {
                require(parent is IrFile)
                val irScript = declarationStorage.createIrScript(declaration)
                addDeclarationToParentIfNeeded(irScript)
                declarationStorage.withScope(irScript.symbol) {
                    irScript.parent = parent
                    for (scriptDeclaration in declaration.declarations.filterIsInstance<FirRegularClass>()) {
                        registerClassAndNestedClasses(scriptDeclaration, irScript)
                    }
                    for (scriptDeclaration in declaration.declarations) {
                        when (scriptDeclaration) {
                            is FirRegularClass -> {
                                processClassAndNestedClassHeaders(scriptDeclaration)
                            }
                            is FirTypeAlias -> classifierStorage.createAndCacheIrTypeAlias(scriptDeclaration, irScript)
                            else -> {}
                        }
                    }
                    for (scriptDeclaration in declaration.declarations) {
                        val needProcessMember = when (scriptDeclaration) {
                            is FirAnonymousInitializer -> false // processed later
                            is FirProperty -> {
                                // '_' DD element
                                scriptDeclaration.name != SpecialNames.UNDERSCORE_FOR_UNUSED_VAR ||
                                        scriptDeclaration.destructuringDeclarationContainerVariable == null
                            }
                            else -> true
                        }
                        if (needProcessMember) {
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
                            declarationStorage.recordSupertypeDelegateFieldMappedToBackingField(delegateField, backingField.symbol)
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
                val irFieldSymbol = if (correspondingClassProperty == null || correspondingClassProperty.isVar) {
                    declarationStorage.createSupertypeDelegateIrField(declaration, parent).symbol
                } else {
                    delegateFieldToPropertyMap.putValue(correspondingClassProperty, declaration)
                    val correspondingIrProperty = declarationStorage.getIrPropertySymbol(correspondingClassProperty.symbol)
                    declarationStorage.findBackingFieldOfProperty(correspondingIrProperty as IrPropertySymbol)
                        ?: error("Backing field not found for property ${correspondingClassProperty.returnTypeRef}")
                }
                val delegationTargetType = declaration.returnTypeRef.toIrType(c)
                declarationStorage.recordSupertypeDelegationInformation(containingClass, parent, delegationTargetType, irFieldSymbol)

            }
            is FirConstructor -> if (!declaration.isPrimary) {
                // the primary constructor was already created in `processClassMembers` function
                declarationStorage.createAndCacheIrConstructor(declaration, { parent as IrClass }, isLocal = isInLocalClass)
            }
            is FirEnumEntry -> {
                classifierStorage.createAndCacheIrEnumEntry(declaration, parent as IrClass)
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
        fun evaluateConstants(irModuleFragment: IrModuleFragment, components: Fir2IrComponents, irBuiltIns: IrBuiltIns) {
            val fir2IrConfiguration = components.configuration
            val firModuleDescriptor = irModuleFragment.descriptor as? FirModuleDescriptor
            val targetPlatform = firModuleDescriptor?.platform
            val languageVersionSettings = firModuleDescriptor?.session?.languageVersionSettings ?: return
            val intrinsicConstEvaluation = languageVersionSettings.supportsFeature(LanguageFeature.IntrinsicConstEvaluation)

            val configuration = IrInterpreterConfiguration(
                platform = targetPlatform,
                printOnlyExceptionMessage = true,
            )

            val interpreter = IrInterpreter(IrInterpreterEnvironment(irBuiltIns, configuration))
            val mode = if (intrinsicConstEvaluation) EvaluationMode.OnlyIntrinsicConst() else EvaluationMode.OnlyBuiltins

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

            fun IrProperty.tryToGetConst(): IrConst? = (backingField?.initializer?.expression as? IrConst)
            fun IrConst.asString(): String {
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
        fun friendModulesMap(session: FirSession): Map<String, List<String>> {
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

        fun generateIrModuleFragment(components: Fir2IrComponentsStorage, firFiles: List<FirFile>): IrModuleFragmentImpl {
            val session = components.session

            session.lazyDeclarationResolver.disableLazyResolveContractChecks()

            val irModuleFragment = IrModuleFragmentImpl(components.moduleDescriptor)

            val allFirFiles = buildList {
                addAll(session.createFilesWithBuiltinsSyntheticDeclarationsIfNeeded())
                addAll(firFiles)
                val generatedFiles = session.createFilesWithGeneratedDeclarations()
                addAll(generatedFiles)
                generatedFiles.forEach { components.firProvider.recordFile(it) }
            }

            components.converter.runSourcesConversion(allFirFiles, irModuleFragment, components.fir2IrVisitor)

            return irModuleFragment
        }
    }
}
