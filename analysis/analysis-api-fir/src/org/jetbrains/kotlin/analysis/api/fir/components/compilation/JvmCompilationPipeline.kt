/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.compilation

import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.kotlin.analysis.api.compilation.KaCompilationResult
import org.jetbrains.kotlin.analysis.api.compilation.KaCompilationTarget
import org.jetbrains.kotlin.analysis.api.compilation.KaCompilerFacilityModuleActualizer
import org.jetbrains.kotlin.analysis.api.compile.KaCodeFragmentCapturedValue
import org.jetbrains.kotlin.analysis.api.fir.compilation.KaFirCompilationOptions
import org.jetbrains.kotlin.analysis.api.fir.components.KaFirSessionComponent
import org.jetbrains.kotlin.analysis.api.fir.components.project
import org.jetbrains.kotlin.analysis.api.fir.utils.KaFirCompiledFileForOutputFile
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinCompilerPluginsProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.baseContextModuleOrSelf
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.ImplementationPlatformKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLKindBasedPlatformActualizer
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLPlatformActualizer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.state.CompiledCodeProvider
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.Fir2IrCommonMemberStorage
import org.jetbrains.kotlin.fir.backend.Fir2IrConfiguration
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmVisibilityConverter
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.utils.hasBody
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.lazy.AbstractFir2IrLazyDeclaration
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.SingleModuleFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.convertToIrAndActualize
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBasedDeclarationDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrBasedReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrBasedValueParameterDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrBasedVariableDescriptor
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.utils.addIfNotNull

context(component: KaFirSessionComponent)
internal fun runJvmIrCodeGen(
    chunk: CompilationChunk,
    fir2IrResult: Fir2IrActualizedResult,
    configuration: CompilerConfiguration,
    options: KaFirCompilationOptions,
    codeFragmentMappings: CodeFragmentMappings?,
    codegenFactory: JvmIrCodegenFactory,
    diagnosticsCollector: BaseDiagnosticsCollector,
    compiledCodeProvider: CompiledCodeProvider,
): KaCompilationResult {
    val matchingClassNames = mutableSetOf<String>()

    val classBuilderFactory = KaClassBuilderFactory.create(
        delegateFactory = if (options.jvmOutputAsmListing) ClassBuilderFactories.TEST else ClassBuilderFactories.BINARIES,
        compiledClassHandler = { file, className ->
            options.compiledClassHandler?.handleClassDefinition(file, className)

            // Synthetic classes often don't have a source element attached, so judging whether the class should stay is hard
            if (chunk.mainFile == null || file == chunk.mainFile) {
                // If any nested class matches the filter, the outer class should also match it.
                // We can meet false positives here: it's OK as it's better than to filter out a required class by a mistake.
                val topLevelClassName = className.substringBefore('$')
                matchingClassNames.add(topLevelClassName)
            }
        }
    )

    val generationState = GenerationState(
        project,
        fir2IrResult.irModuleFragment.descriptor,
        configuration,
        classBuilderFactory,
        jvmBackendClassResolver = FirJvmBackendClassResolver(fir2IrResult.components),
        diagnosticReporter = diagnosticsCollector,
        compiledCodeProvider = compiledCodeProvider
    )

    ProgressManager.checkCanceled()

    val backendInput = JvmIrCodegenFactory.BackendInput(
        fir2IrResult.irModuleFragment,
        fir2IrResult.pluginContext.irBuiltIns,
        fir2IrResult.symbolTable,
        fir2IrResult.components.irProviders,
        CompilerFacilityJvmDebuggerExtensions(),
        FirJvmBackendExtension(fir2IrResult.components, null),
        fir2IrResult.pluginContext,
    )
    codegenFactory.generateModule(generationState, backendInput)

    fun isMatchingRelativeClassPath(path: String): Boolean {
        val className = path.takeIf { it.endsWith(".class", ignoreCase = true) }?.dropLast(".class".length) ?: return false
        return className in matchingClassNames || matchingClassNames.any { className.startsWith("$it$") }
    }

    val compiledFiles = generationState.factory.asList()
        .filter { isMatchingRelativeClassPath(it.relativePath) }
        .map(::KaFirCompiledFileForOutputFile)

    val capturedValues = buildList {
        if (codeFragmentMappings != null) {
            addAll(codeFragmentMappings.capturedValues)
        }
        for ([_, _, descriptor] in generationState.newFragmentCaptureParameters) {
            if (descriptor is IrBasedDeclarationDescriptor<*>) {
                addIfNotNull(computeAdditionalCodeFragmentMapping(descriptor))
            }
        }
    }

    require(compiledFiles.isNotEmpty()) { "Compilation produced no matching output files" }
    return KaCompilationResult.Success(
        compiledFiles,
        capturedValues,
        canBeCached = codeFragmentMappings?.reifiedTypeParametersMapping.isNullOrEmpty()
    )
}

context(component: KaFirSessionComponent)
internal fun getIrGenerationExtensions(module: KaModule): List<IrGenerationExtension> {
    val projectExtensions = project.extensionArea
        .getExtensionPoint<IrGenerationExtension>(IrGenerationExtension.name)
        .extensionList

    val unwrappedModule = module.baseContextModuleOrSelf
    if (unwrappedModule !is KaSourceModule) {
        return projectExtensions
    }

    val moduleExtensions = KotlinCompilerPluginsProvider.getInstance(project)
        ?.getRegisteredExtensions(unwrappedModule, IrGenerationExtension)
        .orEmpty()

    return moduleExtensions + projectExtensions
}

internal fun runFir2Ir(
    session: LLFirSession,
    firFiles: List<FirFile>,
    fir2IrExtensions: Fir2IrExtensions,
    diagnosticReporter: BaseDiagnosticsCollector,
    effectiveConfiguration: CompilerConfiguration,
    irGeneratorExtensions: List<IrGenerationExtension>,
    commonMemberStorage: Fir2IrCommonMemberStorage
): Fir2IrActualizedResult {
    val fir2IrConfiguration =
        Fir2IrConfiguration.forAnalysisApi(effectiveConfiguration, session.languageVersionSettings, diagnosticReporter)
    val frontendOutput = AllModulesFrontendOutput(listOf(SingleModuleFrontendOutput(session, session.getScopeSession(), firFiles)))
    val singleOutput = frontendOutput.outputs.size == 1
    check(singleOutput) { "Single output invariant is used in the lambda below" }

    return frontendOutput.convertToIrAndActualize(
        fir2IrExtensions = fir2IrExtensions,
        fir2IrConfiguration = fir2IrConfiguration,
        irGeneratorExtensions = irGeneratorExtensions,
        irMangler = JvmIrMangler,
        visibilityConverter = FirJvmVisibilityConverter,
        kotlinBuiltIns = DefaultBuiltIns.Instance,
        typeSystemContextProvider = ::JvmIrTypeSystemContext,
        createSpecialAnnotationsProvider = ::JvmIrSpecialAnnotationSymbolProvider,
        extraActualDeclarationExtractorsInitializer = {
            error(
                "extraActualDeclarationExtractorsInitializer should never be called, because outputs is a list of a single element. " +
                        "Output is single => " +
                        "dependentIrFragments will always be empty => " +
                        "IrActualizer will never be called => " +
                        "extraActualDeclarationExtractorsInitializer will never be called"
            )
        },
        commonMemberStorage = commonMemberStorage
    )
}

private fun computeAdditionalCodeFragmentMapping(descriptor: IrBasedDeclarationDescriptor<*>): KaCodeFragmentCapturedValue? {
    val owner = descriptor.owner

    if (descriptor is IrBasedReceiverParameterDescriptor && owner is IrValueParameter) {
        val receiverClass = (owner.type as? IrSimpleType)?.classifier as? IrClassSymbol
        val receiverClassId = receiverClass?.owner?.classId

        val parent = owner.parent
        if (receiverClassId != null && parent is IrFunction) {
            when (owner.kind) {
                IrParameterKind.DispatchReceiver -> {
                    return KaCodeFragmentCapturedValue.ContainingClass(
                        receiverClassId,
                        isCrossingInlineBounds = true,
                        depthRelativeToCurrentFrame = 0
                    )
                }
                IrParameterKind.Context -> {
                    val contextParameterIndex = parent.parameters
                        .subList(0, owner.indexInParameters)
                        .count { it.kind == IrParameterKind.Context }
                    val labelName = receiverClassId.shortClassName
                    return KaCodeFragmentCapturedValue.ContextReceiver(
                        contextParameterIndex,
                        labelName,
                        isCrossingInlineBounds = true,
                        depthRelativeToCurrentFrame = 0
                    )
                }
                IrParameterKind.ExtensionReceiver -> {
                    return KaCodeFragmentCapturedValue.ExtensionReceiver(
                        parent.name.asString(),
                        isCrossingInlineBounds = true,
                        depthRelativeToCurrentFrame = 0
                    )
                }
                IrParameterKind.Regular -> {}
            }
        }
    }

    if (descriptor is IrBasedVariableDescriptor && owner is IrVariable) {
        val name = owner.name
        val isMutated = false // TODO capture the usage somehow

        if (owner.origin == IrDeclarationOrigin.PROPERTY_DELEGATE) {
            return KaCodeFragmentCapturedValue.LocalDelegate(
                name,
                isMutated,
                isCrossingInlineBounds = true,
                depthRelativeToCurrentFrame = 0
            )
        }

        return KaCodeFragmentCapturedValue.Local(name, isMutated, isCrossingInlineBounds = true, depthRelativeToCurrentFrame = 0)
    }

    if (descriptor is IrBasedValueParameterDescriptor && owner is IrValueParameter) {
        val name = owner.name
        return KaCodeFragmentCapturedValue.Local(name, isMutated = false, isCrossingInlineBounds = true, depthRelativeToCurrentFrame = 0)
    }

    return null
}

private class CompilerFacilityJvmDebuggerExtensions : JvmDebuggerExtensions {
    /**
     * This method is used from `org.jetbrains.kotlin.backend.jvm.lower.SpecialAccessLowering.visitCall`
     * (via generateReflectiveAccessForGetter) and it is called for the private access member lowered to the getter/setter call.
     * If a private property has no getter/setter (the typical situation for simple private properties without explicitly defined
     * getter/setter), then this method is not used at all. Instead,
     * `org.jetbrains.kotlin.backend.jvm.lower.SpecialAccessLowering.visitGetField` (or visitSetField) generates the access without
     * asking.
     */
    override fun isAccessorWithExplicitImplementation(accessor: IrSimpleFunction): Boolean {
        if (accessor is AbstractFir2IrLazyDeclaration<*>) {
            val fir = accessor.fir
            if (fir is FirFunction && fir.hasBody) {
                return true
            }
        }
        return false
    }
}

internal fun createJvmIrCodegenFactory(configuration: CompilerConfiguration, evaluatorData: JvmEvaluatorData?): JvmIrCodegenFactory {
    val ideCodegenSettings = JvmIrCodegenFactory.IdeCodegenSettings(
        shouldStubAndNotLinkUnboundSymbols = true,

        // Compilation state acts as an in-out container for captured type parameter and local function mappings
        evaluatorData = evaluatorData
    )

    return JvmIrCodegenFactory(
        configuration,
        ideCodegenSettings = ideCodegenSettings,
    )
}

internal fun createPlatformActualizer(customActualizer: KaCompilerFacilityModuleActualizer?): LLPlatformActualizer {
    val platformKind = ImplementationPlatformKind.JVM

    if (customActualizer != null) {
        return LLPlatformActualizer { module ->
            val actualModule = customActualizer.actualize(module, KaCompilationTarget.JVM)
            if (actualModule != null) {
                val actualPlatform = actualModule.targetPlatform
                val actualPlatformKind = ImplementationPlatformKind.fromTargetPlatform(actualPlatform)
                check(actualPlatformKind == platformKind) {
                    "Expected platform kind $platformKind, got $actualPlatformKind from the platform $actualPlatform"
                }
            }
            actualModule
        }
    }

    return LLKindBasedPlatformActualizer(platformKind)
}
