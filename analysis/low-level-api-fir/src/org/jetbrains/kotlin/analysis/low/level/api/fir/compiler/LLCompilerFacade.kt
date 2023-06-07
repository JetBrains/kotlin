/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.impl.SimpleDiagnosticsCollectorWithSuppress
import org.jetbrains.kotlin.fir.backend.Fir2IrCommonMemberStorage
import org.jetbrains.kotlin.fir.backend.Fir2IrConfiguration
import org.jetbrains.kotlin.fir.backend.Fir2IrConverter
import org.jetbrains.kotlin.fir.backend.jvm.*
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.pipeline.runCheckers
import org.jetbrains.kotlin.fir.pipeline.runResolution
import org.jetbrains.kotlin.fir.pipeline.signatureComposerForJvmFir2Ir
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.source.PsiSourceFile
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class LLCompilationResult(val outputFiles: List<OutputFile>, val diagnostics: List<KtPsiDiagnostic>)

object LLCompilerFacade {
    fun compile(
        file: KtFile,
        configuration: CompilerConfiguration,
        languageVersionSettings: LanguageVersionSettings,
        classBuilderFactory: ClassBuilderFactory,
    ): Result<LLCompilationResult> {
        try {
            val project = file.project

            val effectiveConfiguration = configuration
                .copy()
                .apply { put(JVMConfigurationKeys.DO_NOT_CLEAR_BINDING_CONTEXT, true) }

            val module = ProjectStructureProvider.getModule(project, file, contextualModule = null)

            val resolveSession = module.getFirResolveSession(project)
            val firFile = file.getOrBuildFir(resolveSession) as FirFile
            val scopeSession = resolveSession.useSiteFirSession.runResolution(listOf(firFile)).first

            val inlineCollector = InlineFunctionCollectingVisitor().apply { process(firFile) }

            val filesToCompile = inlineCollector.files
            val firFilesToCompile = filesToCompile.map { it.getOrBuildFir(resolveSession) as FirFile }

            val inlinedClasses = inlineCollector.inlinedClasses
            val filesWithInlinedClasses = inlinedClasses.mapTo(mutableSetOf()) { it.containingKtFile }

            val diagnosticReporter = SimpleDiagnosticsCollectorWithSuppress()
            resolveSession.useSiteFirSession.runCheckers(scopeSession, firFilesToCompile, diagnosticReporter)

            if (diagnosticReporter.hasErrors) {
                val psiDiagnostics = diagnosticReporter.diagnostics.map { it as KtPsiDiagnostic }
                return Result.success(LLCompilationResult(listOf(), psiDiagnostics))
            }

            val generateClassFilter = object : GenerationState.GenerateClassFilter() {
                override fun shouldGeneratePackagePart(ktFile: KtFile): Boolean {
                    return file === ktFile || ktFile in filesWithInlinedClasses
                }

                override fun shouldAnnotateClass(processingClassOrObject: KtClassOrObject): Boolean {
                    return true
                }

                override fun shouldGenerateClass(processingClassOrObject: KtClassOrObject): Boolean {
                    return processingClassOrObject.containingKtFile === file ||
                            processingClassOrObject is KtObjectDeclaration && processingClassOrObject in inlinedClasses
                }

                override fun shouldGenerateScript(script: KtScript): Boolean {
                    return script.containingKtFile === file
                }

                override fun shouldGenerateCodeFragment(script: KtCodeFragment) = false
            }

            val codegenFactory = createJvmIrCodegenFactory(effectiveConfiguration)

            val fir2IrExtensions = JvmFir2IrExtensions(effectiveConfiguration, JvmIrDeserializerImpl(), JvmIrMangler)

            val fir2IrConfiguration = Fir2IrConfiguration(
                languageVersionSettings,
                linkViaSignatures = false,
                EvaluatedConstTracker.create(),
                allowSuddenDeclarations = true
            )

            val irGenerationExtensions = IrGenerationExtension.getInstances(project)

            val fir2IrResult = Fir2IrConverter.createModuleFragmentWithSignaturesIfNeeded(
                resolveSession.useSiteFirSession,
                scopeSession,
                firFilesToCompile,
                fir2IrExtensions,
                fir2IrConfiguration,
                JvmIrMangler,
                IrFactoryImpl,
                FirJvmVisibilityConverter,
                Fir2IrJvmSpecialAnnotationSymbolProvider(),
                irGenerationExtensions,
                DefaultBuiltIns.Instance,
                Fir2IrCommonMemberStorage(signatureComposerForJvmFir2Ir(false), FirJvmKotlinMangler()),
                initializedIrBuiltIns = null
            )

            ProgressManager.checkCanceled()

            val bindingContext = NoScopeRecordCliBindingTrace().bindingContext

            val generationState = GenerationState.Builder(
                project,
                classBuilderFactory,
                fir2IrResult.irModuleFragment.descriptor,
                bindingContext,
                filesToCompile,
                effectiveConfiguration,
            ).generateDeclaredClassFilter(generateClassFilter)
                .codegenFactory(codegenFactory)
                .build()

            try {
                generationState.beforeCompile()

                ProgressManager.checkCanceled()

                codegenFactory.generateModuleInFrontendIRMode(
                    generationState,
                    fir2IrResult.irModuleFragment,
                    fir2IrResult.components.symbolTable,
                    fir2IrResult.components.irProviders,
                    JvmFir2IrExtensions(effectiveConfiguration, JvmIrDeserializerImpl(), JvmIrMangler),
                    FirJvmBackendExtension(fir2IrResult.components, null),
                    fir2IrResult.pluginContext
                )

                CodegenFactory.doCheckCancelled(generationState)
                generationState.factory.done()

                val outputFiles = generationState.factory.asList()

                val backendDiagnostics = buildList {
                    for (diagnostic in generationState.collectedExtraJvmDiagnostics.all()) {
                        if (diagnostic.severity == Severity.ERROR) {
                            add(diagnostic as KtPsiDiagnostic)
                        }
                    }
                }

                return Result.success(LLCompilationResult(outputFiles, backendDiagnostics))
            } finally {
                generationState.destroy()
            }
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Throwable) {
            return Result.failure(e)
        }
    }

    private fun createJvmIrCodegenFactory(configuration: CompilerConfiguration): JvmIrCodegenFactory {
        val jvmGeneratorExtensions = object : JvmGeneratorExtensionsImpl(configuration) {
            override fun getContainerSource(descriptor: DeclarationDescriptor): DeserializedContainerSource? {
                // Stubbed top-level function IR symbols (from other source files in the module) require a parent facade class to be
                // generated, which requires a container source to be provided. Without a facade class, function IR symbols will have
                // an `IrExternalPackageFragment` parent, which trips up code generation during IR lowering.
                val psiSourceFile =
                    descriptor.toSourceElement.containingFile as? PsiSourceFile ?: return super.getContainerSource(descriptor)
                return FacadeClassSourceShimForFragmentCompilation(psiSourceFile)
            }
        }

        val ideCodegenSettings = JvmIrCodegenFactory.IdeCodegenSettings(
            shouldStubAndNotLinkUnboundSymbols = true,
            shouldDeduplicateBuiltInSymbols = true,

            // Because the file to compile may be contained in a "common" multiplatform module, an `expect` declaration doesn't necessarily
            // have an obvious associated `actual` symbol. `shouldStubOrphanedExpectSymbols` generates stubs for such `expect` declarations.
            shouldStubOrphanedExpectSymbols = true,

            // Likewise, the file to compile may be contained in a "platform" multiplatform module, where the `actual` declaration is
            // referenced in the symbol table automatically, but not its `expect` counterpart, because it isn't contained in the files to
            // compile. `shouldReferenceUndiscoveredExpectSymbols` references such `expect` symbols in the symbol table so that they can
            // subsequently be stubbed.
            shouldReferenceUndiscoveredExpectSymbols = false, // TODO it was true
        )

        return JvmIrCodegenFactory(
            configuration,
            PhaseConfig(jvmPhases),
            jvmGeneratorExtensions = jvmGeneratorExtensions,
            ideCodegenSettings = ideCodegenSettings,
        )
    }
}