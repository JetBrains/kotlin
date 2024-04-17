/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import com.intellij.openapi.progress.ProcessCanceledException
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.common.IrSpecialAnnotationsProvider
import org.jetbrains.kotlin.backend.common.actualizer.IrActualizedResult
import org.jetbrains.kotlin.backend.common.actualizer.IrActualizer
import org.jetbrains.kotlin.backend.common.actualizer.SpecialFakeOverrideSymbolsResolver
import org.jetbrains.kotlin.backend.common.actualizer.SpecialFakeOverrideSymbolsResolverVisitor
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.signaturer.FirBasedSignatureComposer
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.overrides.IrFakeOverrideBuilder
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
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
    actualizerTypeContextProvider: (IrBuiltIns) -> IrTypeSystemContext,
    specialAnnotationsProvider: IrSpecialAnnotationsProvider?,
    irModuleFragmentPostCompute: (IrModuleFragment) -> Unit = { _ -> },
): Fir2IrActualizedResult {
    require(outputs.isNotEmpty()) { "No modules found" }

    val commonMemberStorage = Fir2IrCommonMemberStorage(FirBasedSignatureComposer.create(firMangler, fir2IrConfiguration))
    fir2IrExtensions.registerDeclarations(commonMemberStorage.symbolTable)

    val firProvidersWithGeneratedFiles: MutableMap<FirModuleData, FirProviderWithGeneratedFiles> = mutableMapOf()
    for (firOutput in outputs) {
        val session = firOutput.session
        firProvidersWithGeneratedFiles[session.moduleData] = FirProviderWithGeneratedFiles(session, firProvidersWithGeneratedFiles)
    }

    val platformFirOutput = outputs.last()

    fun ModuleCompilerAnalyzedOutput.createFir2IrComponentsStorage(
        irBuiltIns: IrBuiltInsOverFir? = null,
        irTypeSystemContext: IrTypeSystemContext? = null,
    ): Fir2IrComponentsStorage {
        return Fir2IrComponentsStorage(
            session,
            scopeSession,
            fir,
            IrFactoryImpl,
            fir2IrExtensions,
            fir2IrConfiguration,
            visibilityConverter,
            actualizerTypeContextProvider,
            commonMemberStorage,
            irMangler,
            kotlinBuiltIns,
            irBuiltIns,
            specialAnnotationsProvider,
            irTypeSystemContext,
            firProvidersWithGeneratedFiles.getValue(session.moduleData),
        )
    }

    val platformComponentsStorage = platformFirOutput.createFir2IrComponentsStorage()

    val dependentIrFragments = mutableListOf<IrModuleFragment>()
    lateinit var mainIrFragment: IrModuleFragment

    // We need to build all modules before rebuilding fake overrides
    // to avoid fixing declaration storages
    for (firOutput in outputs) {
        val isMainOutput = firOutput === platformFirOutput
        val componentsStorage = if (isMainOutput) {
            platformComponentsStorage
        } else {
            firOutput.createFir2IrComponentsStorage(platformComponentsStorage.irBuiltIns, platformComponentsStorage.irTypeSystemContext)
        }

        val irModuleFragment = Fir2IrConverter.generateIrModuleFragment(componentsStorage, firOutput.fir).also {
            irModuleFragmentPostCompute(it)
        }

        if (isMainOutput) {
            mainIrFragment = irModuleFragment
        } else {
            dependentIrFragments.add(irModuleFragment)
        }
    }

    val irActualizer = if (dependentIrFragments.isEmpty()) null else IrActualizer(
        KtDiagnosticReporterWithImplicitIrBasedContext(
            fir2IrConfiguration.diagnosticReporter,
            fir2IrConfiguration.languageVersionSettings
        ),
        actualizerTypeContextProvider(mainIrFragment.irBuiltins),
        fir2IrConfiguration.expectActualTracker,
        fir2IrConfiguration.useFirBasedFakeOverrideGenerator,
        mainIrFragment,
        dependentIrFragments,
    )

    if (!fir2IrConfiguration.useFirBasedFakeOverrideGenerator) {
        // actualizeCallablesAndMergeModules call below in fact can also actualize classifiers.
        // So to avoid even more changes, when this mode is disabled, we don't run classifiers
        // actualization separately. This should go away, after useIrFakeOverrideBuilder becomes
        // always enabled
        irActualizer?.actualizeClassifiers()
        val temporaryResolver = SpecialFakeOverrideSymbolsResolver(emptyMap())
        platformComponentsStorage.fakeOverrideBuilder.buildForAll(dependentIrFragments + mainIrFragment, temporaryResolver)
    }
    val expectActualMap = irActualizer?.actualizeCallablesAndMergeModules() ?: emptyMap()
    val fakeOverrideResolver = runIf(!platformComponentsStorage.configuration.useFirBasedFakeOverrideGenerator) {
        val fakeOverrideResolver = SpecialFakeOverrideSymbolsResolver(expectActualMap)
        mainIrFragment.acceptVoid(SpecialFakeOverrideSymbolsResolverVisitor(fakeOverrideResolver))
        @OptIn(Fir2IrSymbolsMappingForLazyClasses.SymbolRemapperInternals::class)
        platformComponentsStorage.symbolsMappingForLazyClasses.initializeSymbolMap(fakeOverrideResolver)
        fakeOverrideResolver
    }
    Fir2IrConverter.evaluateConstants(mainIrFragment, platformComponentsStorage)
    val actualizationResult = irActualizer?.runChecksAndFinalize(expectActualMap)

    fakeOverrideResolver?.cacheFakeOverridesOfAllClasses(mainIrFragment)

    val pluginContext = Fir2IrPluginContext(platformComponentsStorage, platformComponentsStorage.moduleDescriptor)
    pluginContext.applyIrGenerationExtensions(mainIrFragment, irGeneratorExtensions)
    return Fir2IrActualizedResult(mainIrFragment, platformComponentsStorage, pluginContext, actualizationResult)
}


private fun resolveOverridenSymbolsInLazyClass(
    clazz: Fir2IrLazyClass,
    resolver: SpecialFakeOverrideSymbolsResolver
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

private fun IrFakeOverrideBuilder.buildForAll(
    modules: List<IrModuleFragment>,
    resolver: SpecialFakeOverrideSymbolsResolver
) {
    val builtFakeOverridesClasses = mutableSetOf<IrClass>()
    fun buildFakeOverrides(clazz: IrClass) {
        if (!builtFakeOverridesClasses.add(clazz)) return
        for (c in clazz.superTypes) {
            c.getClass()?.let { buildFakeOverrides(it) }
        }
        if (clazz is IrLazyDeclarationBase) {
            resolveOverridenSymbolsInLazyClass(clazz as Fir2IrLazyClass, resolver)
        } else {
            buildFakeOverridesForClass(clazz, false)
        }
    }

    class ClassVisitor : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        private fun isIgnoredClass(declaration: IrClass) : Boolean {
            return when {
                declaration.isExpect -> true
                declaration.metadata is MetadataSource.CodeFragment -> true
                else -> false
            }
        }

        override fun visitClass(declaration: IrClass) {
            if (!isIgnoredClass(declaration)) {
                buildFakeOverrides(declaration)
            }
            declaration.acceptChildrenVoid(this)
        }
    }

    for (module in modules) {
        for (file in module.files) {
            try {
                file.acceptVoid(ClassVisitor())
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (e: Throwable) {
                CodegenUtil.reportBackendException(e, "IR fake override builder", file.fileEntry.name) { offset ->
                    file.fileEntry.takeIf { it.supportsDebugInfo }?.let {
                        val (line, column) = it.getLineAndColumnNumbers(offset)
                        line to column
                    }
                }
            }
        }
    }
}

fun IrPluginContext.applyIrGenerationExtensions(irModuleFragment: IrModuleFragment, irGenerationExtensions: Collection<IrGenerationExtension>) {
    if (irGenerationExtensions.isEmpty()) return
    for (extension in irGenerationExtensions) {
        extension.generate(irModuleFragment, this)
    }
}
