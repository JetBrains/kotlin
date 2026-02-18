/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.TARGET_BACKEND
import org.jetbrains.kotlin.test.model.AnalysisHandler
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.model.SourcesKind
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider

abstract class AbstractDirectivesValidatorTest : AbstractKotlinCompilerTest() {
    override fun configure(builder: TestConfigurationBuilder): Unit = with(builder) {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Source
        }

        useAdditionalSourceProviders(
            ::CoroutineHelpersSourceFilesProvider,
        )

        handlersStep(SourcesKind, CompilationStage.FIRST) {
            useHandlers(::DirectivesValidationHandler)
        }
    }
}

private class DirectivesValidationHandler(testServices: TestServices) : AnalysisHandler<ResultingArtifact.Source>(
    testServices,
    failureDisablesNextSteps = false,
    doNotRunIfThereWerePreviousFailures = false,
) {
    override val artifactKind: TestArtifactKind<ResultingArtifact.Source>
        get() = SourcesKind

    override fun processModule(module: TestModule, info: ResultingArtifact.Source) {}

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val directives = testServices.moduleStructure.allDirectives
        verifyTargetBackend(directives[TARGET_BACKEND])
    }

    private fun List<TargetBackend>.notCoveredBackends() = listOf(
        TargetBackend.JVM,
        TargetBackend.JVM_IR,
        TargetBackend.JS_IR,
        TargetBackend.JS_IR_ES6,
        TargetBackend.WASM_JS,
        TargetBackend.WASM_WASI,
        TargetBackend.NATIVE,
    ).filter { backend ->
        backend !in this && backend.compatibleWith !in this
    }


    private fun verifyTargetBackend(backends: List<TargetBackend>) {
        if (backends.size <= 1) return
        if (backends.any { main -> backends.all { it == main || it.compatibleWith == main } }) return
        throw IllegalStateException(
            """
            Don't use TARGET_BACKEND directive with several backends: ${backends}. 
            Use IGNORE_BACKEND or DONT_TARGET_EXACT_BACKEND instead.
            This helps to not forget unmuting tests later.
            Replacement: // IGNORE_BACKEND: ${backends.notCoveredBackends().joinToString(",") { it.name }}
        """
        )
    }
}
