/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.utils.InlineFunctionAnalyzer
import org.jetbrains.kotlin.analysis.api.descriptors.utils.collectReachableInlineDelegatedPropertyAccessors
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.impl.base.components.*
import org.jetbrains.kotlin.analysis.api.impl.base.util.KaBaseCompiledFileForOutputFile
import org.jetbrains.kotlin.backend.jvm.FacadeClassSourceShimForFragmentCompilation
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.source.PsiSourceFile
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.utils.exceptions.rethrowIntellijPlatformExceptionIfNeeded

internal class KaFe10CompilerFacility(
    override val analysisSessionProvider: () -> KaFe10Session
) : KaBaseSessionComponent<KaFe10Session>(), KaCompilerFacility, KaFe10SessionComponent {
    @OptIn(KaImplementationDetail::class)
    override fun compile(file: KtFile, options: KaCompilationOptions): KaCompilationResult {
        val opts = options as KaBaseCompilationOptions
        return withPsiValidityAssertion(file) {
            try {
                compileUnsafe(file, opts)
            } catch (e: Throwable) {
                rethrowIntellijPlatformExceptionIfNeeded(e)
                throw KaCodeCompilationException(e)
            }
        }
    }

    @OptIn(KaImplementationDetail::class, CompilerConfiguration.Internals::class)
    override fun createCompilationOptions(init: KaCompilerOptionsBuilder.() -> Unit): KaCompilationOptions {
        return KaBaseCompilerOptionsBuilder(token, CompilerConfiguration.create()).apply(init).build()
    }

    @OptIn(KaImplementationDetail::class)
    override fun KaCompilationOptions.copy(init: KaCompilerOptionsBuilder.() -> Unit): KaCompilationOptions {
        return (this as KaBaseCompilationOptions).copy(init)
    }

    private fun compileUnsafe(
        file: KtFile,
        options: KaBaseCompilationOptions,
    ): KaCompilationResult {
        if (file is KtCodeFragment) {
            throw UnsupportedOperationException("Code fragments are not supported in K1 implementation")
        }

        val effectiveConfiguration = options.configuration
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

        val frontendErrors = computeErrors(bindingContext.diagnostics, options.allowedErrorFilter)
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

            override fun shouldGenerateClass(processingClassOrObject: KtClassOrObject): Boolean {
                return processingClassOrObject.containingKtFile === file ||
                        processingClassOrObject is KtObjectDeclaration && inlineObjectDeclarations.contains(processingClassOrObject)
            }
        }

        val generateClassFilter = GenerateClassFilter()

        val codegenFactory = createJvmIrCodegenFactory(effectiveConfiguration, options.stubUnboundIrSymbols)

        val classBuilderFactory = KaClassBuilderFactory.create(
            delegateFactory = if (options.jvmOutputAsmListing) ClassBuilderFactories.TEST else ClassBuilderFactories.BINARIES,
            compiledClassHandler = options.compiledClassHandler
        )

        val state = GenerationState(
            file.project,
            analysisContext.resolveSession.moduleDescriptor,
            effectiveConfiguration,
            classBuilderFactory,
            generateDeclaredClassFilter = generateClassFilter,
        )

        codegenFactory.convertAndGenerate(filesToCompile, state, bindingContext)
        val outputFiles = state.factory.asList().map(::KaBaseCompiledFileForOutputFile)
        return KaCompilationResult.Success(outputFiles, capturedValues = emptyList(), canBeCached = true)
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

    private fun createJvmIrCodegenFactory(configuration: CompilerConfiguration, stubUnboundIrSymbols: Boolean): JvmIrCodegenFactory {

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
            jvmGeneratorExtensions = jvmGeneratorExtensions,
            ideCodegenSettings = ideCodegenSettings,
        )
    }
}