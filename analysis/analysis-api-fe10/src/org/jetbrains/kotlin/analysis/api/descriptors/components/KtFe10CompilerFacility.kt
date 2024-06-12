/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KaCodeCompilationException
import org.jetbrains.kotlin.analysis.api.components.KaCompilationResult
import org.jetbrains.kotlin.analysis.api.components.KaCompilerFacility
import org.jetbrains.kotlin.analysis.api.components.KaCompilerTarget
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.utils.InlineFunctionAnalyzer
import org.jetbrains.kotlin.analysis.api.descriptors.utils.collectReachableInlineDelegatedPropertyAccessors
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.util.KaCompiledFileForOutputFile
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.FacadeClassSourceShimForFragmentCompilation
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.jvmPhases
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.source.PsiSourceFile
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.utils.exceptions.rethrowIntellijPlatformExceptionIfNeeded

/**
 * Whether unbound IR symbols should be stubbed instead of linked.
 * This should be enabled if the compiled file could refer to symbols defined in another file of the same module.
 * Such symbols are not compiled (only the file is passed to the backend) and so they cannot be linked from a dependency.
 */
val STUB_UNBOUND_IR_SYMBOLS: CompilerConfigurationKey<Boolean> = CompilerConfigurationKey<Boolean>("stub unbound IR symbols")

internal class KaFe10CompilerFacility(
    override val analysisSessionProvider: () -> KaFe10Session
) : KaSessionComponent<KaFe10Session>(), KaCompilerFacility, KaFe10SessionComponent {
    override fun compile(
        file: KtFile,
        configuration: CompilerConfiguration,
        target: KaCompilerTarget,
        allowedErrorFilter: (KaDiagnostic) -> Boolean
    ): KaCompilationResult = withValidityAssertion {
        try {
            compileUnsafe(file, configuration, target, allowedErrorFilter)
        } catch (e: Throwable) {
            rethrowIntellijPlatformExceptionIfNeeded(e)
            throw KaCodeCompilationException(e)
        }
    }

    private fun compileUnsafe(
        file: KtFile,
        configuration: CompilerConfiguration,
        target: KaCompilerTarget,
        allowedErrorFilter: (KaDiagnostic) -> Boolean
    ): KaCompilationResult {
        if (file is KtCodeFragment) {
            throw UnsupportedOperationException("Code fragments are not supported in K1 implementation")
        }

        val classBuilderFactory = when (target) {
            is KaCompilerTarget.Jvm -> target.classBuilderFactory
        }

        val effectiveConfiguration = configuration
            .copy()
            .apply {
                put(JVMConfigurationKeys.DO_NOT_CLEAR_BINDING_CONTEXT, true)
            }

        val disableInline = effectiveConfiguration.getBoolean(CommonConfigurationKeys.DISABLE_INLINE)

        // The binding context needs to be built from all files with reachable inline functions, as such files may contain classes whose
        // descriptors must be available in the binding context for the IR backend. Note that the full bytecode is only generated for
        // `file` because of filtering in `generateClassFilter`, while only select declarations from other files are generated if needed
        // by the backend.
        val inlineAnalyzer = InlineFunctionAnalyzer(analysisContext, analyzeOnlyReifiedInlineFunctions = disableInline)
        inlineAnalyzer.analyze(file)

        val filesToCompile = inlineAnalyzer.allFiles().collectReachableInlineDelegatedPropertyAccessors()
        val bindingContext = analysisContext.analyze(filesToCompile, AnalysisMode.ALL_COMPILER_CHECKS)

        val frontendErrors = computeErrors(bindingContext.diagnostics, allowedErrorFilter)
        if (frontendErrors.isNotEmpty()) {
            return KaCompilationResult.Failure(frontendErrors)
        }

        // The IR backend will try to regenerate object literals defined in inline functions from generated class files during inlining.
        // Hence, we need to be aware of which object declarations are defined in the relevant inline functions.
        val inlineObjectDeclarations = inlineAnalyzer.inlineObjectDeclarations()
        val inlineObjectDeclarationFiles = inlineObjectDeclarations.mapTo(mutableSetOf()) { it.containingKtFile }

        class GenerateClassFilter : GenerationState.GenerateClassFilter() {
            override fun shouldGeneratePackagePart(ktFile: KtFile): Boolean {
                return file === ktFile || inlineObjectDeclarationFiles.contains(ktFile)
            }

            override fun shouldAnnotateClass(processingClassOrObject: KtClassOrObject): Boolean {
                return true
            }

            override fun shouldGenerateClass(processingClassOrObject: KtClassOrObject): Boolean {
                return processingClassOrObject.containingKtFile === file ||
                        processingClassOrObject is KtObjectDeclaration && inlineObjectDeclarations.contains(processingClassOrObject)
            }

            override fun shouldGenerateScript(script: KtScript): Boolean {
                return script.containingKtFile === file
            }

            override fun shouldGenerateCodeFragment(script: KtCodeFragment) = false
        }

        val generateClassFilter = GenerateClassFilter()

        val codegenFactory = createJvmIrCodegenFactory(effectiveConfiguration)

        val state = GenerationState.Builder(
            file.project,
            classBuilderFactory,
            analysisContext.resolveSession.moduleDescriptor,
            bindingContext,
            filesToCompile,
            effectiveConfiguration,
        ).generateDeclaredClassFilter(generateClassFilter)
            .codegenFactory(codegenFactory)
            .build()

        try {
            KotlinCodegenFacade.compileCorrectFiles(state)

            val backendErrors = computeErrors(state.collectedExtraJvmDiagnostics, allowedErrorFilter)
            if (backendErrors.isNotEmpty()) {
                return KaCompilationResult.Failure(backendErrors)
            }

            val outputFiles = state.factory.asList().map(::KaCompiledFileForOutputFile)
            return KaCompilationResult.Success(outputFiles, capturedValues = emptyList())
        } finally {
            state.destroy()
        }
    }

    private fun computeErrors(diagnostics: Diagnostics, allowedErrorFilter: (KaDiagnostic) -> Boolean): List<KaDiagnostic> {
        return buildList {
            for (diagnostic in diagnostics.all()) {
                if (diagnostic.severity == Severity.ERROR) {
                    val ktDiagnostic = KaFe10Diagnostic(diagnostic, token)
                    if (!allowedErrorFilter(ktDiagnostic)) {
                        add(ktDiagnostic)
                    }
                }
            }
        }
    }

    private fun createJvmIrCodegenFactory(configuration: CompilerConfiguration): JvmIrCodegenFactory {
        val stubUnboundIrSymbols = configuration[STUB_UNBOUND_IR_SYMBOLS] == true

        val jvmGeneratorExtensions = if (stubUnboundIrSymbols) {
            object : JvmGeneratorExtensionsImpl(configuration) {
                override fun getContainerSource(descriptor: DeclarationDescriptor): DeserializedContainerSource? {
                    // Stubbed top-level function IR symbols (from other source files in the module) require a parent facade class to be
                    // generated, which requires a container source to be provided. Without a facade class, function IR symbols will have
                    // an `IrExternalPackageFragment` parent, which trips up code generation during IR lowering.
                    val psiSourceFile =
                        descriptor.toSourceElement.containingFile as? PsiSourceFile ?: return super.getContainerSource(descriptor)
                    return FacadeClassSourceShimForFragmentCompilation(psiSourceFile)
                }
            }
        } else {
            JvmGeneratorExtensionsImpl(configuration)
        }

        val ideCodegenSettings = JvmIrCodegenFactory.IdeCodegenSettings(
            shouldStubAndNotLinkUnboundSymbols = stubUnboundIrSymbols,
            shouldDeduplicateBuiltInSymbols = stubUnboundIrSymbols,

            // Because the file to compile may be contained in a "common" multiplatform module, an `expect` declaration doesn't necessarily
            // have an obvious associated `actual` symbol. `shouldStubOrphanedExpectSymbols` generates stubs for such `expect` declarations.
            shouldStubOrphanedExpectSymbols = true,

            // Likewise, the file to compile may be contained in a "platform" multiplatform module, where the `actual` declaration is
            // referenced in the symbol table automatically, but not its `expect` counterpart, because it isn't contained in the files to
            // compile. `shouldReferenceUndiscoveredExpectSymbols` references such `expect` symbols in the symbol table so that they can
            // subsequently be stubbed.
            shouldReferenceUndiscoveredExpectSymbols = true,
        )

        return JvmIrCodegenFactory(
            configuration,
            PhaseConfig(jvmPhases),
            jvmGeneratorExtensions = jvmGeneratorExtensions,
            ideCodegenSettings = ideCodegenSettings,
        )
    }
}