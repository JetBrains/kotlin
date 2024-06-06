/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import com.intellij.openapi.progress.ProcessCanceledException
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.common.IrSpecialAnnotationsProvider
import org.jetbrains.kotlin.backend.common.actualizer.*
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.generators.Fir2IrDataClassGeneratedMemberBodyGenerator
import org.jetbrains.kotlin.fir.backend.utils.generatedBuiltinsDeclarationsFileName
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.overrides.IrFakeOverrideBuilder
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.utils.addToStdlib.runIf

data class FirResult(val outputs: List<ModuleCompilerAnalyzedOutput>)

data class ModuleCompilerAnalyzedOutput(
    val session: FirSession,
    val scopeSession: ScopeSession,
    val fir: List<FirFile>,
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
    firMangler: FirMangler,
    visibilityConverter: Fir2IrVisibilityConverter,
    kotlinBuiltIns: KotlinBuiltIns,
    typeSystemContextProvider: (IrBuiltIns) -> IrTypeSystemContext,
    specialAnnotationsProvider: IrSpecialAnnotationsProvider?,
    extraActualDeclarationExtractorInitializer: (Fir2IrComponents) -> IrExtraActualDeclarationExtractor?,
    irModuleFragmentPostCompute: (IrModuleFragment) -> Unit = { _ -> },
): Fir2IrActualizedResult {
    val pipeline = Fir2IrPipeline(
        outputs,
        fir2IrExtensions,
        fir2IrConfiguration,
        irGeneratorExtensions,
        irMangler,
        firMangler,
        visibilityConverter,
        kotlinBuiltIns,
        typeSystemContextProvider,
        specialAnnotationsProvider,
        extraActualDeclarationExtractorInitializer,
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
    val firMangler: FirMangler,
    val visibilityConverter: Fir2IrVisibilityConverter,
    val kotlinBuiltIns: KotlinBuiltIns,
    val typeSystemContextProvider: (IrBuiltIns) -> IrTypeSystemContext,
    val specialAnnotationsProvider: IrSpecialAnnotationsProvider?,
    val extraActualDeclarationExtractorInitializer: (Fir2IrComponents) -> IrExtraActualDeclarationExtractor?,
    val irModuleFragmentPostCompute: (IrModuleFragment) -> Unit,
) {
    private class Fir2IrConversionResult(
        val mainIrFragment: IrModuleFragmentImpl,
        val dependentIrFragments: List<IrModuleFragmentImpl>,
        val componentsStorage: Fir2IrComponentsStorage,
        val commonMemberStorage: Fir2IrCommonMemberStorage,
        val irBuiltIns: IrBuiltIns,
        val symbolTable: SymbolTable,
        val irTypeSystemContext: IrTypeSystemContext,
    )

    fun convertToIrAndActualize(): Fir2IrActualizedResult {
        require(outputs.isNotEmpty()) { "No modules found" }

        val fir2IrOutput = runFir2IrConversion()
        return fir2IrOutput.runActualizationPipeline()
    }

    private fun runFir2IrConversion(): Fir2IrConversionResult {
        val commonMemberStorage = Fir2IrCommonMemberStorage(firMangler)

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
                IrFactoryImpl,
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

        mainIrFragment.initializeIrBuiltins(irBuiltIns)
        dependentIrFragments.forEach { it.initializeIrBuiltins(irBuiltIns) }

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
        val irBuiltIns = IrBuiltInsOverFir(
            componentsStorage,
            FirModuleDescriptor.createSourceModuleDescriptor(componentsStorage.session, kotlinBuiltIns),
            syntheticIrBuiltinsSymbolsContainer,
        )
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

        val pluginContext = Fir2IrPluginContext(componentsStorage, irBuiltIns, componentsStorage.moduleDescriptor, symbolTable)
        pluginContext.applyIrGenerationExtensions(mainIrFragment, irGeneratorExtensions)

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
                this@Fir2IrPipeline.extraActualDeclarationExtractorInitializer(componentsStorage),
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
        delegatedMembersGenerationStrategy.generateDelegatedBodies()
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
    }

    private fun Fir2IrConversionResult.resolveFakeOverrideSymbols(fakeOverrideResolver: SpecialFakeOverrideSymbolsResolver) {
        mainIrFragment.acceptVoid(SpecialFakeOverrideSymbolsResolverVisitor(fakeOverrideResolver))

        val expectActualMap = fakeOverrideResolver.expectActualMap
        if (expectActualMap.propertyAccessorsActualizedByFields.isNotEmpty()) {
            mainIrFragment.transform(SpecialFakeOverrideSymbolsActualizedByFieldsTransformer(expectActualMap), null)
        }

        @OptIn(Fir2IrSymbolsMappingForLazyClasses.SymbolRemapperInternals::class)
        componentsStorage.symbolsMappingForLazyClasses.initializeSymbolMap(fakeOverrideResolver)
    }

    private fun Fir2IrConversionResult.evaluateConstants() {
        Fir2IrConverter.evaluateConstants(mainIrFragment, componentsStorage)
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
                c.getClass()?.let { buildFakeOverrides(it) }
            }
            if (clazz is IrLazyDeclarationBase) {
                //resolveOverridenSymbolsInLazyClass(clazz as Fir2IrLazyClass, resolver)
                buildFakeOverridesForLazyClass(clazz as Fir2IrLazyClass, resolver)
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

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun buildFakeOverridesForLazyClass(
        clazz: Fir2IrLazyClass,
        resolver: SpecialFakeOverrideSymbolsResolver,
    ) {
        val declarationStorage = clazz.declarationStorage
        val mainScope = clazz.fir.unsubstitutedScope(clazz)
        val lookupTag = clazz.fir.symbol.toLookupTag()

        val allFromSuper = clazz.superTypes.flatMap { superType ->
            superType.classOrFail.owner.declarations
        }


        listOfNotNull(mainScope, clazz.fir.staticScopeForBackend(clazz.session, clazz.scopeSession)).forEach { scope ->
            val functionNames = mutableSetOf<Name>()
            val propertyNames = mutableSetOf<Name>()
            for (decl in allFromSuper) {
                when (decl) {
                    is IrSimpleFunction -> functionNames.add(decl.name)
                    is IrProperty -> propertyNames.add(decl.name)
                }
            }

            for (name in functionNames) {
                scope.processFunctionsByName(name) { function ->
                    declarationStorage.getIrFunctionSymbol(function, lookupTag)
                }
            }
            for (name in propertyNames) {
                scope.processPropertiesByName(name) { property ->
                    if (property is FirPropertySymbol) {
                        declarationStorage.getIrPropertySymbol(property, lookupTag)
                    }
                }
            }
        }

        /*for (decl in allFromSuper) {
            when (decl) {
                is IrSimpleFunction -> {
                    decl as Fir2IrLazySimpleFunction

                    scope.processOverriddenFunctionsAndSelf(decl.fir.symbol) { func ->
                        fakeOverrides += declarationStorage.getIrFunctionSymbol(func, lookupTag).owner as IrSimpleFunction
                        ProcessorAction.NEXT
                    }
                }
                is IrProperty -> {
                    decl as Fir2IrLazyProperty
                    scope.processOverriddenProperties(decl.fir.symbol) { property ->
                        fakeOverrides += declarationStorage.getIrPropertySymbol(property, lookupTag).owner as IrProperty
                        ProcessorAction.NEXT
                    }
                }
            }
        }*/
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

fun IrPluginContext.applyIrGenerationExtensions(
    irModuleFragment: IrModuleFragment,
    irGenerationExtensions: Collection<IrGenerationExtension>,
) {
    if (irGenerationExtensions.isEmpty()) return
    for (extension in irGenerationExtensions) {
        extension.generate(irModuleFragment, this)
    }
}
