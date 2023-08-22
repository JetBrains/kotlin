/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtPsiSourceFileLinesMapping
import org.jetbrains.kotlin.KtSourceFileLinesMappingFromLineStartOffsets
import org.jetbrains.kotlin.backend.common.CommonBackendErrors
import org.jetbrains.kotlin.backend.common.sourceElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.backend.generators.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.declarations.utils.isSynthetic
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.extensions.declarationGenerators
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.generatedMembers
import org.jetbrains.kotlin.fir.extensions.generatedNestedClassifiers
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.symbols.Fir2IrConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.Fir2IrSimpleFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.lazyDeclarationResolver
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.ir.PsiIrFileEntry
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterConfiguration
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterEnvironment
import org.jetbrains.kotlin.ir.interpreter.checker.EvaluationMode
import org.jetbrains.kotlin.ir.interpreter.transformer.transformConst
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile

class Fir2IrConverter(
    private val moduleDescriptor: FirModuleDescriptor,
    private val components: Fir2IrComponents
) : Fir2IrComponents by components {

    private val generatorExtensions = session.extensionService.declarationGenerators

    private var wereSourcesFakeOverridesBound = false
    private val postponedDeclarationsForFakeOverridesBinding = mutableListOf<IrDeclaration>()

    private fun runSourcesConversion(
        allFirFiles: List<FirFile>,
        irModuleFragment: IrModuleFragmentImpl,
        fir2irVisitor: Fir2IrVisitor,
        runPreCacheBuiltinClasses: Boolean
    ) {
        for (firFile in allFirFiles) {
            registerFileAndClasses(firFile, irModuleFragment)
        }
        if (runPreCacheBuiltinClasses) {
            classifierStorage.preCacheBuiltinClasses()
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

        evaluateConstants(irModuleFragment, configuration)
    }

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
        processClassAndNestedClassHeaders(klass)
        return irClass
    }

    fun processLocalClassAndNestedClasses(klass: FirClass, parent: IrDeclarationParent): IrClass {
        val irClass = registerClassAndNestedClasses(klass, parent)
        processClassAndNestedClassHeaders(klass)
        processClassMembers(klass, irClass)
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
                is FirCodeFragment -> classifierStorage.registerCodeFragmentClass(declaration, irFile)
                else -> {}
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

    fun processAnonymousObjectHeaders(
        anonymousObject: FirAnonymousObject,
        irClass: IrClass,
    ) {
        registerNestedClasses(anonymousObject, irClass)
        processNestedClassHeaders(anonymousObject)
    }

    internal fun processClassMembers(klass: FirClass, irClass: IrClass): IrClass {
        val allDeclarations = mutableListOf<FirDeclaration>().apply {
            addAll(klass.declarations)
            if (klass is FirRegularClass && generatorExtensions.isNotEmpty()) {
                addAll(klass.generatedMembers(session))
                addAll(klass.generatedNestedClassifiers(session))
            }
        }
        val irConstructor = klass.primaryConstructorIfAny(session)?.let {
            declarationStorage.getOrCreateIrConstructor(
                it.fir, irClass, isLocal = klass.isLocal
            )
        }
        if (irConstructor != null) {
            irClass.declarations += irConstructor
        }
        // At least on enum entry creation we may need a default constructor, so ctors should be converted first
        for (declaration in syntheticPropertiesLast(allDeclarations)) {
            val irDeclaration = processMemberDeclaration(declaration, klass, irClass) ?: continue
            irClass.declarations += irDeclaration
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
        with(fakeOverrideGenerator) {
            irClass.addFakeOverrides(klass, allDeclarations)
        }

        return irClass
    }

    private fun processCodeFragmentMembers(
        codeFragment: FirCodeFragment,
        irClass: IrClass = classifierStorage.getCachedIrCodeFragment(codeFragment)!!
    ): IrClass {
        val conversionData = codeFragment.conversionData

        declarationStorage.enterScope(irClass.symbol)

        val signature = irClass.symbol.signature!!

        val irPrimaryConstructor = symbolTable.declareConstructor(signature, { Fir2IrConstructorSymbol(signature) }) { irSymbol ->
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
                parent = irClass
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

        val irFragmentFunction = symbolTable.declareSimpleFunction(signature, { Fir2IrSimpleFunctionSymbol(signature) }) { irSymbol ->
            val lastStatement = codeFragment.block.statements.lastOrNull()
            val returnType = (lastStatement as? FirExpression)?.coneTypeOrNull?.toIrType(typeConverter) ?: irBuiltIns.unitType

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
                parent = irClass
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

        irClass.declarations.add(irPrimaryConstructor)
        irClass.declarations.add(irFragmentFunction)

        declarationStorage.leaveScope(irClass.symbol)
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
                is FirRegularClass -> classifierStorage.registerIrClass(klass, parent)
                is FirAnonymousObject -> classifierStorage.registerIrAnonymousObject(klass, irParent = parent)
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
        classifierStorage.processClassHeader(klass)
        processNestedClassHeaders(klass)
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

    private fun processMemberDeclaration(
        declaration: FirDeclaration,
        containingClass: FirClass?,
        parent: IrDeclarationParent
    ): IrDeclaration? {
        val isLocal = containingClass != null &&
                (containingClass !is FirRegularClass || containingClass.isLocal)
        return when (declaration) {
            is FirRegularClass -> {
                processClassMembers(declaration, classifierStorage.getCachedIrClass(declaration)!!)
            }
            is FirScript -> {
                parent as IrFile
                declarationStorage.getOrCreateIrScript(declaration).also { irScript ->
                    declarationStorage.enterScope(irScript.symbol)
                    irScript.parent = parent
                    for (scriptStatement in declaration.statements) {
                        when (scriptStatement) {
                            is FirRegularClass -> {
                                registerClassAndNestedClasses(scriptStatement, irScript)
                                processClassAndNestedClassHeaders(scriptStatement)
                            }
                            is FirTypeAlias -> classifierStorage.registerTypeAlias(scriptStatement, irScript)
                            else -> {}
                        }
                    }
                    for (scriptStatement in declaration.statements) {
                        if (scriptStatement is FirDeclaration) {
                            processMemberDeclaration(scriptStatement, null, irScript)
                        }
                    }
                    declarationStorage.leaveScope(irScript.symbol)
                }
            }
            is FirSimpleFunction -> {
                declarationStorage.getOrCreateIrFunction(
                    declaration, parent, isLocal = isLocal
                )
            }
            is FirProperty -> {
                if (containingClass != null &&
                    declaration.isEnumEntries(containingClass) &&
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
                classifierStorage.getIrEnumEntry(declaration, parent as IrClass)
            }
            is FirAnonymousInitializer -> {
                declarationStorage.createIrAnonymousInitializer(declaration, parent as IrClass)
            }
            is FirTypeAlias -> {
                // DO NOTHING
                null
            }
            is FirCodeFragment -> {
                processCodeFragmentMembers(declaration)
            }
            else -> {
                error("Unexpected member: ${declaration::class}")
            }
        }
    }

    companion object {
        private fun evaluateConstants(irModuleFragment: IrModuleFragment, fir2IrConfiguration: Fir2IrConfiguration) {
            val firModuleDescriptor = irModuleFragment.descriptor as? FirModuleDescriptor
            val targetPlatform = firModuleDescriptor?.platform
            val languageVersionSettings = firModuleDescriptor?.session?.languageVersionSettings ?: return
            val intrinsicConstEvaluation = languageVersionSettings.supportsFeature(LanguageFeature.IntrinsicConstEvaluation) == true

            val configuration = IrInterpreterConfiguration(
                platform = targetPlatform,
                printOnlyExceptionMessage = true,
            )
            val interpreter = IrInterpreter(IrInterpreterEnvironment(irModuleFragment.irBuiltins, configuration))
            val mode = if (intrinsicConstEvaluation) EvaluationMode.ONLY_INTRINSIC_CONST else EvaluationMode.ONLY_BUILTINS
            val ktDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(fir2IrConfiguration.diagnosticReporter, languageVersionSettings)
            irModuleFragment.files.forEach {
                it.transformConst(
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

        fun createModuleFragmentWithSignaturesIfNeeded(
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
            initializedIrBuiltIns: IrBuiltInsOverFir?
        ): Fir2IrResult {
            session.lazyDeclarationResolver.disableLazyResolveContractChecks()
            val moduleDescriptor = FirModuleDescriptor(session, kotlinBuiltIns)
            val components = Fir2IrComponentsStorage(
                session,
                scopeSession,
                commonMemberStorage.symbolTable,
                irFactory,
                commonMemberStorage.firSignatureComposer,
                fir2IrExtensions,
                fir2IrConfiguration,
            )
            val converter = Fir2IrConverter(moduleDescriptor, components)

            components.converter = converter
            components.classifierStorage = Fir2IrClassifierStorage(components, commonMemberStorage)
            components.delegatedMemberGenerator = DelegatedMemberGenerator(components)
            components.declarationStorage = Fir2IrDeclarationStorage(components, moduleDescriptor, commonMemberStorage)
            components.visibilityConverter = visibilityConverter
            components.typeConverter = Fir2IrTypeConverter(components)
            val irBuiltIns = initializedIrBuiltIns ?: IrBuiltInsOverFir(
                components, fir2IrConfiguration.languageVersionSettings, moduleDescriptor, irMangler
            )
            components.irBuiltIns = irBuiltIns
            val conversionScope = Fir2IrConversionScope(components.configuration)
            val fir2irVisitor = Fir2IrVisitor(components, conversionScope)
            components.builtIns = Fir2IrBuiltIns(components, specialSymbolProvider)
            components.annotationGenerator = AnnotationGenerator(components)
            components.fakeOverrideGenerator = FakeOverrideGenerator(components, conversionScope)
            components.callGenerator = CallAndReferenceGenerator(components, fir2irVisitor, conversionScope)
            components.irProviders = listOf(FirIrProvider(components))
            components.annotationsFromPluginRegistrar = Fir2IrAnnotationsFromPluginRegistrar(components)

            fir2IrExtensions.registerDeclarations(commonMemberStorage.symbolTable)

            val irModuleFragment = IrModuleFragmentImpl(moduleDescriptor, irBuiltIns)

            val allFirFiles = buildList {
                addAll(firFiles)
                addAll(session.createFilesWithGeneratedDeclarations())
            }

            converter.runSourcesConversion(
                allFirFiles, irModuleFragment, fir2irVisitor, runPreCacheBuiltinClasses = initializedIrBuiltIns == null
            )

            return Fir2IrResult(irModuleFragment, components, moduleDescriptor)
        }
    }
}
