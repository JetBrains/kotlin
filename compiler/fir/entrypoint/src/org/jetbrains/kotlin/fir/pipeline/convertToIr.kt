/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.backend.common.IrSpecialAnnotationsProvider
import org.jetbrains.kotlin.backend.common.IrValidatorConfig
import org.jetbrains.kotlin.backend.common.actualizer.*
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.validateIr
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.generators.Fir2IrDataClassGeneratedMemberBodyGenerator
import org.jetbrains.kotlin.fir.backend.utils.generatedBuiltinsDeclarationsFileName
import org.jetbrains.kotlin.fir.backend.utils.unsubstitutedScope
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyProperty
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyPropertyAccessor
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyPropertyForPureField
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazySimpleFunction
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.staticScopeForBackend
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyClass
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.irFlag
import org.jetbrains.kotlin.ir.overrides.IrFakeOverrideBuilder
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.utils.addToStdlib.runIf

data class FirResult(val outputs: List<ModuleCompilerAnalyzedOutput>)

data class ModuleCompilerAnalyzedOutput(
    val session: FirSession,
    val scopeSession: ScopeSession,
    val fir: List<FirFile>
)

data class Fir2IrActualizedResult(
    val irModuleFragment: IrModuleFragment,
    val components: Fir2IrComponents,
    val pluginContext: Fir2IrPluginContext,
    val irActualizedResult: IrActualizedResult?,
    val irBuiltIns: IrBuiltIns,
    val symbolTable: SymbolTable,
)

fun List<ModuleCompilerAnalyzedOutput>.runPlatformCheckers(reporter: BaseDiagnosticsCollector) {
    val platformModule = this.last()
    val session = platformModule.session
    val scopeSession = platformModule.scopeSession

    val allFiles = this.flatMap { it.fir }
    session.runCheckers(scopeSession, allFiles, reporter, MppCheckerKind.Platform)
}

fun FirResult.convertToIrAndActualize(
    fir2IrExtensions: Fir2IrExtensions,
    fir2IrConfiguration: Fir2IrConfiguration,
    irGeneratorExtensions: Collection<IrGenerationExtension>,
    irMangler: KotlinMangler.IrMangler,
    visibilityConverter: Fir2IrVisibilityConverter,
    kotlinBuiltIns: KotlinBuiltIns,
    typeSystemContextProvider: (IrBuiltIns) -> IrTypeSystemContext,
    specialAnnotationsProvider: IrSpecialAnnotationsProvider?,
    extraActualDeclarationExtractorsInitializer: (Fir2IrComponents) -> List<IrExtraActualDeclarationExtractor>,
    irModuleFragmentPostCompute: (IrModuleFragment) -> Unit = { _ -> },
): Fir2IrActualizedResult {
    val pipeline = Fir2IrPipeline(
        outputs,
        fir2IrExtensions,
        fir2IrConfiguration,
        irGeneratorExtensions,
        irMangler,
        visibilityConverter,
        kotlinBuiltIns,
        typeSystemContextProvider,
        specialAnnotationsProvider,
        extraActualDeclarationExtractorsInitializer,
        irModuleFragmentPostCompute
    )
    return pipeline.convertToIrAndActualize()
}

private class Fir2IrPipeline(
    val outputs: List<ModuleCompilerAnalyzedOutput>,
    val fir2IrExtensions: Fir2IrExtensions,
    val fir2IrConfiguration: Fir2IrConfiguration,
    val irGeneratorExtensions: Collection<IrGenerationExtension>,
    val irMangler: KotlinMangler.IrMangler,
    val visibilityConverter: Fir2IrVisibilityConverter,
    val kotlinBuiltIns: KotlinBuiltIns,
    val typeSystemContextProvider: (IrBuiltIns) -> IrTypeSystemContext,
    val specialAnnotationsProvider: IrSpecialAnnotationsProvider?,
    val extraActualDeclarationExtractorsInitializer: (Fir2IrComponents) -> List<IrExtraActualDeclarationExtractor>,
    val irModuleFragmentPostCompute: (IrModuleFragment) -> Unit,
) {
    private class Fir2IrConversionResult(
        val mainIrFragment: IrModuleFragmentImpl,
        val dependentIrFragments: List<IrModuleFragmentImpl>,
        val componentsStorage: Fir2IrComponentsStorage,
        val commonMemberStorage: Fir2IrCommonMemberStorage,
        val irBuiltIns: IrBuiltIns,
        val symbolTable: SymbolTable,
        val irTypeSystemContext: IrTypeSystemContext
    )

    fun convertToIrAndActualize(): Fir2IrActualizedResult {
        require(outputs.isNotEmpty()) { "No modules found" }

        val fir2IrOutput = runFir2IrConversion()
        return fir2IrOutput.runActualizationPipeline()
    }

    private fun runFir2IrConversion(): Fir2IrConversionResult {
        val commonMemberStorage = Fir2IrCommonMemberStorage()

        val firProvidersWithGeneratedFiles: MutableMap<FirModuleData, FirProviderWithGeneratedFiles> = mutableMapOf()
        for (firOutput in outputs) {
            val session = firOutput.session
            firProvidersWithGeneratedFiles[session.moduleData] = FirProviderWithGeneratedFiles(session, firProvidersWithGeneratedFiles)
        }

        val syntheticIrBuiltinsSymbolsContainer = Fir2IrSyntheticIrBuiltinsSymbolsContainer()

        lateinit var componentsStorage: Fir2IrComponentsStorage

        val fragments = outputs.map {
            componentsStorage = Fir2IrComponentsStorage(
                it.session,
                it.scopeSession,
                it.fir,
                fir2IrExtensions,
                fir2IrConfiguration,
                visibilityConverter,
                commonMemberStorage,
                irMangler,
                kotlinBuiltIns,
                specialAnnotationsProvider,
                firProvidersWithGeneratedFiles.getValue(it.session.moduleData),
                syntheticIrBuiltinsSymbolsContainer,
            )

            Fir2IrConverter.generateIrModuleFragment(componentsStorage, it.fir).also { moduleFragment ->
                irModuleFragmentPostCompute(moduleFragment)
            }
        }

        val dependentIrFragments = fragments.dropLast(1)
        val mainIrFragment = fragments.last()
        val (irBuiltIns, symbolTable) = createBuiltInsAndSymbolTable(componentsStorage, syntheticIrBuiltinsSymbolsContainer)

        val irTypeSystemContext = typeSystemContextProvider(irBuiltIns)

        return Fir2IrConversionResult(
            mainIrFragment,
            dependentIrFragments,
            componentsStorage,
            commonMemberStorage,
            irBuiltIns,
            symbolTable,
            irTypeSystemContext
        )
    }

    private fun createBuiltInsAndSymbolTable(
        componentsStorage: Fir2IrComponentsStorage,
        syntheticIrBuiltinsSymbolsContainer: Fir2IrSyntheticIrBuiltinsSymbolsContainer,
    ): Pair<IrBuiltIns, SymbolTable> {
        val irBuiltIns = IrBuiltInsOverFir(componentsStorage, syntheticIrBuiltinsSymbolsContainer)
        val symbolTable = SymbolTable(signaturer = null, IrFactoryImpl, lock = componentsStorage.lock)

        fir2IrExtensions.initializeIrBuiltInsAndSymbolTable(irBuiltIns, symbolTable)

        return irBuiltIns to symbolTable
    }

    private fun Fir2IrConversionResult.runActualizationPipeline(): Fir2IrActualizedResult {
        val irActualizer = createIrActualizer()

        // actualizeCallablesAndMergeModules call below in fact can also actualize classifiers.
        // So to avoid even more changes, when this mode is disabled, we don't run classifiers
        // actualization separately. This should go away, after useIrFakeOverrideBuilder becomes
        // always enabled
        irActualizer?.actualizeClassifiers()

        generateSyntheticBodiesOfDataValueMembers()

        val (fakeOverrideStrategy, delegatedMembersGenerationStrategy) = buildFakeOverridesAndPlatformSpecificDeclarations(irActualizer)

        val expectActualMap = irActualizer?.actualizeCallablesAndMergeModules() ?: IrExpectActualMap()

        val pluginContext = Fir2IrPluginContext(
            componentsStorage, irBuiltIns, componentsStorage.moduleDescriptor, symbolTable,
            fir2IrConfiguration.messageCollector, fir2IrConfiguration.diagnosticReporter
        )
        if (fir2IrConfiguration.diagnosticReporter.hasErrors) {
            irActualizer?.runChecksAndFinalize(expectActualMap)
            return Fir2IrActualizedResult(
                mainIrFragment, componentsStorage, pluginContext, irActualizedResult = null, irBuiltIns, symbolTable
            )
        }

        val fakeOverrideResolver = SpecialFakeOverrideSymbolsResolver(expectActualMap)
        resolveFakeOverrideSymbols(fakeOverrideResolver)
        delegatedMembersGenerationStrategy.updateMetadataSources(
            commonMemberStorage.firClassesWithInheritanceByDelegation,
            outputs.last().session,
            outputs.last().scopeSession,
            componentsStorage.declarationStorage,
            fakeOverrideResolver
        )

        evaluateConstants()

        val actualizationResult = irActualizer?.runChecksAndFinalize(expectActualMap)

        fakeOverrideResolver.cacheFakeOverridesOfAllClasses(mainIrFragment)
        fakeOverrideStrategy.clearFakeOverrideFields()

        removeGeneratedBuiltinsDeclarationsIfNeeded()

        pluginContext.applyIrGenerationExtensions(fir2IrConfiguration, mainIrFragment, irGeneratorExtensions)

        return Fir2IrActualizedResult(mainIrFragment, componentsStorage, pluginContext, actualizationResult, irBuiltIns, symbolTable)
    }

    // ------------------------------------------------------ pipeline steps ------------------------------------------------------

    private fun Fir2IrConversionResult.createIrActualizer(): IrActualizer? {
        return runIf(dependentIrFragments.isNotEmpty()) {
            IrActualizer(
                KtDiagnosticReporterWithImplicitIrBasedContext(
                    fir2IrConfiguration.diagnosticReporter,
                    fir2IrConfiguration.languageVersionSettings
                ),
                irTypeSystemContext,
                fir2IrConfiguration.expectActualTracker,
                mainIrFragment,
                dependentIrFragments,
                this@Fir2IrPipeline.extraActualDeclarationExtractorsInitializer(componentsStorage),
            )
        }

    }

    private fun Fir2IrConversionResult.generateSyntheticBodiesOfDataValueMembers() {
        Fir2IrDataClassGeneratedMemberBodyGenerator(irBuiltIns)
            .generateBodiesForClassesWithSyntheticDataClassMembers(
                commonMemberStorage.generatedDataValueClassSyntheticFunctions,
                symbolTable
            )
    }

    private fun Fir2IrConversionResult.createFakeOverrideBuilder(
        irActualizer: IrActualizer?
    ): Pair<IrFakeOverrideBuilder, Fir2IrDelegatedMembersGenerationStrategy> {
        val session = componentsStorage.session
        val delegatedMembersGenerationStrategy = Fir2IrDelegatedMembersGenerationStrategy(
            symbolTable.irFactory, irBuiltIns, fir2IrExtensions, commonMemberStorage.delegatedClassesInfo,
            irActualizer?.classActualizationInfo,
        )
        return IrFakeOverrideBuilder(
            irTypeSystemContext,
            Fir2IrFakeOverrideStrategy(
                Fir2IrConverter.friendModulesMap(session),
                isGenericClashFromSameSupertypeAllowed = session.moduleData.platform.isJvm(),
                isOverrideOfPublishedApiFromOtherModuleDisallowed = session.moduleData.platform.isJvm(),
                delegatedMembersGenerationStrategy,
            ),
            componentsStorage.extensions.externalOverridabilityConditions
        ) to delegatedMembersGenerationStrategy
    }

    private fun Fir2IrConversionResult.buildFakeOverridesAndPlatformSpecificDeclarations(
        irActualizer: IrActualizer?
    ): Pair<Fir2IrFakeOverrideStrategy, Fir2IrDelegatedMembersGenerationStrategy> {
        val (fakeOverrideBuilder, delegatedMembersGenerationStrategy) = createFakeOverrideBuilder(irActualizer)
        buildFakeOverrides(fakeOverrideBuilder)
        if (!componentsStorage.configuration.skipBodies) {
            delegatedMembersGenerationStrategy.generateDelegatedBodies()
        }

        val fakeOverrideStrategy = fakeOverrideBuilder.strategy as Fir2IrFakeOverrideStrategy
        return fakeOverrideStrategy to delegatedMembersGenerationStrategy
    }

    private fun Fir2IrConversionResult.buildFakeOverrides(fakeOverrideBuilder: IrFakeOverrideBuilder) {
        val temporaryResolver = SpecialFakeOverrideSymbolsResolver(IrExpectActualMap())
        val getExternalPackages = {
            componentsStorage.declarationStorage.fragmentCache.values
                .flatMap { it.fragmentsForDependencies.values + it.builtinFragmentsForDependencies.values + it.fragmentForPrecompiledBinaries }
        }

        fakeOverrideBuilder.buildForAll(dependentIrFragments + mainIrFragment, getExternalPackages, temporaryResolver)
        @OptIn(Fir2IrSymbolsMappingForLazyClasses.SymbolRemapperInternals::class)
        componentsStorage.symbolsMappingForLazyClasses.initializeRemapper(temporaryResolver)
    }

    private fun Fir2IrConversionResult.resolveFakeOverrideSymbols(fakeOverrideResolver: SpecialFakeOverrideSymbolsResolver) {
        mainIrFragment.acceptVoid(SpecialFakeOverrideSymbolsResolverVisitor(fakeOverrideResolver))

        val expectActualMap = fakeOverrideResolver.expectActualMap
        if (expectActualMap.propertyAccessorsActualizedByFields.isNotEmpty()) {
            mainIrFragment.transform(SpecialFakeOverrideSymbolsActualizedByFieldsTransformer(expectActualMap), null)
        }

        // TODO: remove this and create a correct remapper from the beginnning: KT-70907
        @OptIn(Fir2IrSymbolsMappingForLazyClasses.SymbolRemapperInternals::class)
        componentsStorage.symbolsMappingForLazyClasses.unregisterRemapper()
        @OptIn(Fir2IrSymbolsMappingForLazyClasses.SymbolRemapperInternals::class)
        componentsStorage.symbolsMappingForLazyClasses.initializeRemapper(fakeOverrideResolver)
    }

    private fun Fir2IrConversionResult.evaluateConstants() {
        Fir2IrConverter.evaluateConstants(mainIrFragment, componentsStorage, irBuiltIns)
    }

    // ------------------------------------------------------ f/o building helpers ------------------------------------------------------

    private fun IrFakeOverrideBuilder.buildForAll(
        modules: List<IrModuleFragment>,
        getExternalPackages: () -> List<IrExternalPackageFragment>,
        resolver: SpecialFakeOverrideSymbolsResolver,
    ) {
        val builtFakeOverridesClasses = mutableSetOf<IrClass>()
        fun buildFakeOverrides(clazz: IrClass) {
            if (!builtFakeOverridesClasses.add(clazz)) return
            for (c in clazz.superTypes) {
                c.getClass()?.let { superClass ->
                    if (superClass is Fir2IrLazyClass) {
                        superClass.isSubclassedInCompiledCode = true
                        superClass.computeDeclarationsNeededForSubclasses()
                    }
                    buildFakeOverrides(superClass)
                }
            }
            if (clazz is IrLazyDeclarationBase) {
                resolveOverridenSymbolsInLazyClass(clazz as Fir2IrLazyClass, resolver)
            } else {
                buildFakeOverridesForClass(clazz, false)
            }
        }

        class ClassVisitor : IrElementVisitorVoid {
            val allClasses = hashSetOf<IrClass>()
            val classesToProcess = mutableListOf<IrClass>()

            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitDeclaration(declaration: IrDeclarationBase) {
                if (declaration is IrLazyDeclarationBase) {
                    if (declaration is Fir2IrLazyClass) {
                        @OptIn(UnsafeDuringIrConstructionAPI::class)
                        declaration.declarations.forEach { it.acceptVoid(this) }
                    }
                } else {
                    super.visitDeclaration(declaration)
                }
            }

            private fun isIgnoredClass(declaration: IrClass): Boolean {
                return when {
                    declaration.isExpect -> true
                    declaration.metadata is MetadataSource.CodeFragment -> true
                    else -> false
                }
            }

            override fun visitClass(declaration: IrClass) {
                if (!isIgnoredClass(declaration)) {
                    if (allClasses.add(declaration)) {
                        classesToProcess += declaration
                    }
                }
                super.visitClass(declaration)
            }
        }

        val visitor = ClassVisitor()
        val roots: MutableList<IrPackageFragment> = modules.flatMap { it.files }.toMutableList()
        while (true) {
            roots += getExternalPackages()
            for (root in roots) {
                root.acceptVoid(visitor)
            }
            roots.clear()

            if (visitor.classesToProcess.isEmpty()) break
            for (clazz in visitor.classesToProcess) {
                buildFakeOverrides(clazz)
            }
            visitor.classesToProcess.clear()
        }
    }

    private fun resolveOverridenSymbolsInLazyClass(
        clazz: Fir2IrLazyClass,
        resolver: SpecialFakeOverrideSymbolsResolver,
    ) {
        /*
         * Eventually, we should be able to process lazy classes with the same code.
         *
         * Now we can't do this, because overriding by Java function is not supported correctly in IR builder.
         * In most cases, nothing need to be done for lazy classes. For other cases, it is
         * caller responsibility to handle them.
         *
         * Super-classes already have processed fake overrides at this moment.
         * Also, all Fir2IrLazyClass super-classes are always platform classes,
         * so it's valid to process it with empty expect-actual mapping.
         *
         * But this is still a hack, and should be removed within KT-64352
         */
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        for (declaration in clazz.declarations) {
            when (declaration) {
                is IrSimpleFunction -> {
                    declaration.overriddenSymbols = declaration.overriddenSymbols.map { resolver.getReferencedSimpleFunction(it) }
                }
                is IrProperty -> {
                    declaration.overriddenSymbols = declaration.overriddenSymbols.map { resolver.getReferencedProperty(it) }
                    declaration.getter?.let { getter ->
                        getter.overriddenSymbols = getter.overriddenSymbols.map { resolver.getReferencedSimpleFunction(it) }
                    }
                    declaration.setter?.let { setter ->
                        setter.overriddenSymbols = setter.overriddenSymbols.map { resolver.getReferencedSimpleFunction(it) }
                    }
                }
            }
        }
    }


    /** If `stdlibCompilation` mode is enabled, there could be files with synthetic declarations.
     *  All of them should be generated before FIR2IR conversion and removed after the actualizaiton.
     */
    private fun Fir2IrConversionResult.removeGeneratedBuiltinsDeclarationsIfNeeded() {
        if (fir2IrConfiguration.languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation)) {
            mainIrFragment.files.removeAll { it.name == generatedBuiltinsDeclarationsFileName }
        }
    }
}

private fun IrPluginContext.runMandatoryIrValidation(
    extension: IrGenerationExtension?,
    module: IrModuleFragment,
    fir2IrConfiguration: Fir2IrConfiguration,
) {
    if (!fir2IrConfiguration.validateIrAfterPlugins) return
    // TODO(KT-71138): Replace with IrVerificationMode.ERROR in Kotlin 2.2
    validateIr(fir2IrConfiguration.messageCollector, IrVerificationMode.WARNING) {
        customMessagePrefix = if (extension == null) {
            "The frontend generated invalid IR. This is a compiler bug, please report it to https://kotl.in/issue."
        } else {
            "The compiler plugin '${extension.javaClass.name}' generated invalid IR. Please report this bug to the plugin vendor."
        }
        performBasicIrValidation(
            module,
            irBuiltIns,
            phaseName = "",
            IrValidatorConfig(
                // Invalid parents and duplicated IR nodes don't always result in broken KLIBs,
                // so we disable them not to cause too much breakage.
                checkTreeConsistency = false,
                // Cross-file field accesses, though, do result in invalid KLIBs, so report them as early as possible.
                checkCrossFileFieldUsage = true,
                // FIXME(KT-71243): This should be true, but currently the ExplicitBackingFields feature de-facto allows specifying
                //  non-private visibilities for fields.
                checkAllKotlinFieldsArePrivate = !fir2IrConfiguration.languageVersionSettings.supportsFeature(LanguageFeature.ExplicitBackingFields),
            )
        )
    }
}

fun IrPluginContext.applyIrGenerationExtensions(
    fir2IrConfiguration: Fir2IrConfiguration,
    irModuleFragment: IrModuleFragment,
    irGenerationExtensions: Collection<IrGenerationExtension>,
) {
    runMandatoryIrValidation(null, irModuleFragment, fir2IrConfiguration)
    for (extension in irGenerationExtensions) {
        extension.generate(irModuleFragment, this)
        runMandatoryIrValidation(extension, irModuleFragment, fir2IrConfiguration)
    }
}

private var Fir2IrLazyClass.isSubclassedInCompiledCode: Boolean by irFlag(false)