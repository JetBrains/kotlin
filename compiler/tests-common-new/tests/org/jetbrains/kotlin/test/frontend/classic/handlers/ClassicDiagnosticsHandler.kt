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
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactoryImpl
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.MARK_DYNAMIC_CALLS
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.REPORT_JVM_DIAGNOSTICS_ON_FRONTEND
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

    private val reporter = ClassicDiagnosticReporter(testServices)

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
        val configuration = reporter.createConfiguration(module)

        for ((file, ktFile) in info.ktFiles) {
            val diagnostics = diagnosticsPerFile[ktFile] ?: emptyList()
            for (diagnostic in diagnostics) {
                if (!diagnostic.isValid) continue
                if (!diagnosticsService.shouldRenderDiagnostic(module, diagnostic.factory.name, diagnostic.severity)) continue
                reporter.reportDiagnostic(diagnostic, module, file, configuration, withNewInferenceModeEnabled)
            }
            for (errorElement in AnalyzingUtils.getSyntaxErrorRanges(ktFile)) {
                reporter.reportDiagnostic(SyntaxErrorDiagnostic(errorElement), module, file, configuration, withNewInferenceModeEnabled)
            }
            processDebugInfoDiagnostics(configuration, module, file, ktFile, info, withNewInferenceModeEnabled)
        }
    }

    private fun computeJvmSignatureDiagnostics(info: ClassicFrontendOutputArtifact): Set<Diagnostic> {
        if (testServices.moduleStructure.modules.any { !it.targetPlatform.isJvm() }) return emptySet()
        if (REPORT_JVM_DIAGNOSTICS_ON_FRONTEND !in testServices.moduleStructure.allDirectives) return emptySet()
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
        val onlyExplicitlyDefined = DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO in module.directives
        for (debugAnnotation in debugAnnotations) {
            val factory = debugAnnotation.diagnostic.factory
            if (!diagnosticsService.shouldRenderDiagnostic(module, factory.name, factory.severity)) continue
            if (onlyExplicitlyDefined && !debugAnnotation.diagnostic.textRanges.any { it.startOffset..it.endOffset in diagnosedRanges }) {
                continue
            }
            reporter.reportDiagnostic(debugAnnotation.diagnostic, module, file, configuration, withNewInferenceModeEnabled)
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
