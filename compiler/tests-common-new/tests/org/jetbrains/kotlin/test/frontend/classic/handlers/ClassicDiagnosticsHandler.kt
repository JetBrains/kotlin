/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic.handlers

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.getJvmSignatureDiagnostics
import org.jetbrains.kotlin.checkers.diagnostics.SyntaxErrorDiagnostic
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.checkers.utils.DiagnosticsRenderingConfiguration
import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import org.jetbrains.kotlin.codeMetaInfo.model.DiagnosticCodeMetaInfo
import org.jetbrains.kotlin.codeMetaInfo.model.ParsedCodeMetaInfo
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactoryImpl
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.MARK_DYNAMIC_CALLS
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import java.util.*

class ClassicDiagnosticsHandler(testServices: TestServices) : ClassicFrontendAnalysisHandler(testServices) {
    override val directivesContainers: List<DirectivesContainer> =
        listOf(DiagnosticsDirectives)

    override val additionalServices: List<ServiceRegistrationData> =
        listOf(service(::DiagnosticsService))

    private val globalMetadataInfoHandler: GlobalMetadataInfoHandler
        get() = testServices.globalMetadataInfoHandler

    private val diagnosticsService: DiagnosticsService
        get() = testServices.diagnosticsService

    @OptIn(ExperimentalStdlibApi::class)
    override fun processModule(module: TestModule, info: ClassicFrontendOutputArtifact) {
        var allDiagnostics = info.analysisResult.bindingContext.diagnostics + computeJvmSignatureDiagnostics(info)
        if (AdditionalFilesDirectives.CHECK_TYPE in module.directives) {
            allDiagnostics = allDiagnostics.filter { it.factory.name != Errors.UNDERSCORE_USAGE_WITHOUT_BACKTICKS.name }
        }
        if (LanguageSettingsDirectives.API_VERSION in module.directives) {
            allDiagnostics = allDiagnostics.filter { it.factory.name != Errors.NEWER_VERSION_IN_SINCE_KOTLIN.name }
        }

        val diagnosticsPerFile = allDiagnostics.groupBy { it.psiFile }

        val withNewInferenceModeEnabled = testServices.withNewInferenceModeEnabled()

        val configuration = DiagnosticsRenderingConfiguration(
            platform = null,
            withNewInference = info.languageVersionSettings.supportsFeature(LanguageFeature.NewInference),
            languageVersionSettings = info.languageVersionSettings,
            skipDebugInfoDiagnostics = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
                .getBoolean(JVMConfigurationKeys.IR)
        )

        for ((file, ktFile) in info.ktFiles) {
            val diagnostics = diagnosticsPerFile[ktFile] ?: emptyList()
            for (diagnostic in diagnostics) {
                if (!diagnostic.isValid) continue
                if (!diagnosticsService.shouldRenderDiagnostic(module, diagnostic.factory.name)) continue
                globalMetadataInfoHandler.addMetadataInfosForFile(
                    file,
                    diagnostic.toMetaInfo(
                        module,
                        file,
                        configuration.withNewInference,
                        withNewInferenceModeEnabled
                    )
                )
            }
            for (errorElement in AnalyzingUtils.getSyntaxErrorRanges(ktFile)) {
                globalMetadataInfoHandler.addMetadataInfosForFile(
                    file,
                    SyntaxErrorDiagnostic(errorElement).toMetaInfo(
                        module,
                        file,
                        configuration.withNewInference,
                        withNewInferenceModeEnabled
                    )
                )
            }
            processDebugInfoDiagnostics(configuration, module, file, ktFile, info, withNewInferenceModeEnabled)
        }
    }

    private fun computeJvmSignatureDiagnostics(info: ClassicFrontendOutputArtifact): Set<Diagnostic> {
        if (testServices.moduleStructure.modules.any { !it.targetPlatform.isJvm() }) return emptySet()
        val bindingContext = info.analysisResult.bindingContext
        val project = info.project
        val jvmSignatureDiagnostics = HashSet<Diagnostic>()
        for (ktFile in info.ktFiles.values) {
            val declarations = PsiTreeUtil.findChildrenOfType(ktFile, KtDeclaration::class.java)
            for (declaration in declarations) {
                val diagnostics = getJvmSignatureDiagnostics(
                    declaration,
                    bindingContext.diagnostics,
                    GlobalSearchScope.allScope(project)
                ) ?: continue
                jvmSignatureDiagnostics.addAll(diagnostics.forElement(declaration))
            }
        }
        return jvmSignatureDiagnostics
    }

    private fun Diagnostic.toMetaInfo(
        module: TestModule,
        file: TestFile,
        newInferenceEnabled: Boolean,
        withNewInferenceModeEnabled: Boolean
    ): List<DiagnosticCodeMetaInfo> = textRanges.map { range ->
        val metaInfo = DiagnosticCodeMetaInfo(range, ClassicMetaInfoUtils.renderDiagnosticNoArgs, this)
        if (withNewInferenceModeEnabled) {
            metaInfo.attributes += if (newInferenceEnabled) OldNewInferenceMetaInfoProcessor.NI else OldNewInferenceMetaInfoProcessor.OI
        }
        if (file !in module.files) {
            val targetPlatform = module.targetPlatform
            metaInfo.attributes += when {
                targetPlatform.isJvm() -> "JVM"
                targetPlatform.isJs() -> "JS"
                targetPlatform.isNative() -> "NATIVE"
                targetPlatform.isCommon() -> "COMMON"
                else -> error("Should not be here")
            }
        }
        val existing = globalMetadataInfoHandler.getExistingMetaInfosForActualMetadata(file, metaInfo)
        if (existing.any { it.description != null }) {
            metaInfo.replaceRenderConfiguration(ClassicMetaInfoUtils.renderDiagnosticWithArgs)
        }
        metaInfo
    }

    private fun processDebugInfoDiagnostics(
        configuration: DiagnosticsRenderingConfiguration,
        module: TestModule,
        file: TestFile,
        ktFile: KtFile,
        info: ClassicFrontendOutputArtifact,
        withNewInferenceModeEnabled: Boolean
    ) {
        val diagnosedRanges = globalMetadataInfoHandler.getExistingMetaInfosForFile(file)
            .groupBy(
                keySelector = { it.start..it.end },
                valueTransform = { it.tag }
            )
            .mapValues { (_, it) -> it.toMutableSet() }
        val debugAnnotations = CheckerTestUtil.getDebugInfoDiagnostics(
            ktFile,
            info.analysisResult.bindingContext,
            markDynamicCalls = MARK_DYNAMIC_CALLS in module.directives,
            dynamicCallDescriptors = mutableListOf(),
            configuration,
            dataFlowValueFactory = DataFlowValueFactoryImpl(info.languageVersionSettings),
            info.analysisResult.moduleDescriptor as ModuleDescriptorImpl,
            diagnosedRanges = diagnosedRanges
        )
        debugAnnotations.mapNotNull { debugAnnotation ->
            if (!diagnosticsService.shouldRenderDiagnostic(module, debugAnnotation.diagnostic.factory.name)) return@mapNotNull null
            globalMetadataInfoHandler.addMetadataInfosForFile(
                file,
                debugAnnotation.diagnostic.toMetaInfo(module, file, configuration.withNewInference, withNewInferenceModeEnabled)
            )
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}

class OldNewInferenceMetaInfoProcessor(testServices: TestServices) : AdditionalMetaInfoProcessor(testServices) {
    companion object {
        const val OI = "OI"
        const val NI = "NI"
    }

    override fun processMetaInfos(module: TestModule, file: TestFile) {
        /*
         * Rules for OI/NI attribute:
         * ┌──────────┬──────┬──────┬──────────┐
         * │          │  OI  │  NI  │ nothing  │ <- reported
         * ├──────────┼──────┼──────┼──────────┤
         * │  nothing │ both │ both │ nothing  │
         * │    OI    │  OI  │ both │   OI     │
         * │    NI    │ both │  NI  │   NI     │
         * │   both   │ both │ both │ opposite │ <- OI if NI enabled in test and vice versa
         * └──────────┴──────┴──────┴──────────┘
         *       ^ existed
         */
        if (!testServices.withNewInferenceModeEnabled()) return
        val newInferenceEnabled = module.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)
        val (currentFlag, otherFlag) = when (newInferenceEnabled) {
            true -> NI to OI
            false -> OI to NI
        }
        val matchedExistedInfos = mutableSetOf<ParsedCodeMetaInfo>()
        val matchedReportedInfos = mutableSetOf<CodeMetaInfo>()
        val allReportedInfos = globalMetadataInfoHandler.getReportedMetaInfosForFile(file)
        for ((_, reportedInfos) in allReportedInfos.groupBy { Triple(it.start, it.end, it.tag) }) {
            val existedInfos = globalMetadataInfoHandler.getExistingMetaInfosForActualMetadata(file, reportedInfos.first())
            for ((reportedInfo, existedInfo) in reportedInfos.zip(existedInfos)) {
                matchedExistedInfos += existedInfo
                matchedReportedInfos += reportedInfo
                if (currentFlag !in reportedInfo.attributes) continue
                if (currentFlag in existedInfo.attributes) continue
                reportedInfo.attributes.remove(currentFlag)
            }
        }

        if (allReportedInfos.size != matchedReportedInfos.size) {
            for (info in allReportedInfos) {
                if (info !in matchedReportedInfos) {
                    info.attributes.remove(currentFlag)
                }
            }
        }

        val allExistedInfos = globalMetadataInfoHandler.getExistingMetaInfosForFile(file)
        if (allExistedInfos.size == matchedExistedInfos.size) return

        val newInfos = allExistedInfos.mapNotNull {
            if (it in matchedExistedInfos) return@mapNotNull null
            if (currentFlag in it.attributes) return@mapNotNull null
            it.copy().apply {
                if (otherFlag !in attributes) {
                    attributes += otherFlag
                }
            }
        }
        globalMetadataInfoHandler.addMetadataInfosForFile(file, newInfos)
    }
}

private fun TestServices.withNewInferenceModeEnabled(): Boolean {
    return DiagnosticsDirectives.WITH_NEW_INFERENCE in moduleStructure.allDirectives
}
