/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirCustomScriptDefinitionTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.compiler.plugin.services.scriptDefinitionProviderService
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.runReadAction
import org.jetbrains.kotlin.scripting.resolve.InvalidScriptResolverAnnotation
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.scripting.resolve.getScriptCollectedData
import org.jetbrains.kotlin.scripting.resolve.refineScriptCompilationConfiguration
import org.jetbrains.kotlin.scripting.test.definitions.testScriptDefinitionClasspath
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import java.lang.reflect.Method
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCollectedData
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.collectedAnnotations
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.api.with
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.test.assertTrue

/**
 * For testing script configuration refinement of a PSI-based script in an Analysis API environment:
 * - via the legacy PSI-based entry points ([getScriptCollectedData] and [refineScriptCompilationConfiguration]) currently used in IJ,
 *   without any FIR session and with the classpath of the script definition passed in the configuration;
 * - in the session of the script module, via `FirScriptDefinitionProviderService`, where the annotations are resolved on top of the
 *   libraries of the script module.
 */
abstract class AbstractScriptConfigurationRefinementTest : AbstractAnalysisApiBasedTest() {

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val k1FrontendUsageDetector = K1FrontendUsageDetector(codeUnderTest = KtFileScriptSource::class.java, K1_CONSTANT_EVALUATOR_CLASS)

        val definition = runReadAction { mainFile.findScriptDefinition() }
            ?: error("No script definition is found for ${mainFile.name}")

        val legacyConfiguration = definition.compilationConfiguration.with {
            dependencies.append(JvmDependency(testScriptDefinitionClasspath))
        }
        val collectedData = runReadAction {
            getScriptCollectedData(mainFile, legacyConfiguration, definition.contextClassLoader)
        }
        val collectedAnnotations = collectedData[ScriptCollectedData.collectedAnnotations].orEmpty()

        val refined = refineScriptCompilationConfiguration(KtFileScriptSource(mainFile), definition, mainFile.project, legacyConfiguration)

        val refinedInSession = withResolutionFacade(mainFile) { resolutionFacade ->
            resolutionFacade.useSiteFirSession.scriptDefinitionProviderService?.getRefinedConfiguration(KtFileScriptSource(mainFile))
        }

        val actual = buildString {
            appendLine("-- collected annotations --")
            if (collectedAnnotations.isEmpty()) {
                appendLine("<none>")
            } else {
                collectedAnnotations.forEach { appendLine(renderAnnotation(it.annotation)) }
            }
            appendLine()
            appendLine("-- refinement --")
            appendLine(renderRefinementResult(refined))
            appendLine()
            appendLine("-- refinement in the script session --")
            appendLine(renderSessionRefinementResult(refinedInSession))
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)

        k1FrontendUsageDetector.assertK1FrontendIsNotUsed()
    }

    // the result type of the PSI-based refinement API is deprecated, but it is the one used by the IDE
    @Suppress("DEPRECATION")
    private fun renderRefinementResult(result: ResultWithDiagnostics<ScriptCompilationConfigurationWrapper>): String =
        buildString {
            when (result) {
                is ResultWithDiagnostics.Success -> {
                    appendLine("status: success")
                    appendLine("defaultImports: ${renderDefaultImports(result.value.configuration)}")
                }
                is ResultWithDiagnostics.Failure -> appendLine("status: failure")
            }
            result.reports.forEach { appendLine("${it.severity}: ${it.message}") }
        }.trimEnd()

    private fun renderSessionRefinementResult(result: ResultWithDiagnostics<ScriptCompilationConfiguration>?): String =
        buildString {
            when (result) {
                null -> appendLine("status: not available")
                is ResultWithDiagnostics.Success -> {
                    appendLine("status: success")
                    appendLine("defaultImports: ${renderDefaultImports(result.value)}")
                }
                is ResultWithDiagnostics.Failure -> appendLine("status: failure")
            }
            result?.reports?.forEach { appendLine("${it.severity}: ${it.message}") }
        }.trimEnd()

    private fun renderDefaultImports(configuration: ScriptCompilationConfiguration?): String =
        configuration?.get(ScriptCompilationConfiguration.defaultImports).orEmpty().toString()

    private fun renderAnnotation(annotation: Annotation): String = when (annotation) {
        is InvalidScriptResolverAnnotation ->
            "INVALID(${annotation.name}, error=${annotation.error?.message})"
        else -> {
            val annotationClass = annotation.annotationClass.java
            val arguments = annotationClass.declaredMethods.sortedBy { it.name }.joinToString {
                "${it.name} = ${renderValue(it.invoke(annotation))}"
            }
            "@${annotationClass.simpleName}($arguments)"
        }
    }

    private fun renderValue(value: Any?): String = when (value) {
        is Array<*> -> value.joinToString(prefix = "[", postfix = "]") { renderValue(it) }
        is String -> "\"$value\""
        else -> value.toString()
    }
}

private const val K1_CONSTANT_EVALUATOR_CLASS = "org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator"

/**
 * Detects the use of the K1 frontend, which might soon be absent in the IDE, by checking whether any of the [k1Classes] gets loaded by
 * the class loader of [codeUnderTest].
 *
 * TODO(KT-89679): replace with a real class loading failure once `:compiler:frontend` is removed from the Analysis API test runtime classpath
 */
private class K1FrontendUsageDetector(private val codeUnderTest: Class<*>, private vararg val k1Classes: String) {
    private val classLoaders = generateSequence(codeUnderTest.classLoader) { it.parent }.toList()

    private val findLoadedClass: Method =
        ClassLoader::class.java.getDeclaredMethod("findLoadedClass", String::class.java).apply { isAccessible = true }

    private val loadedInitially: Set<String> = loadedK1Classes()

    private fun loadedK1Classes(): Set<String> =
        k1Classes.filterTo(HashSet()) { name -> classLoaders.any { findLoadedClass.invoke(it, name) != null } }

    fun assertK1FrontendIsNotUsed() {
        val loaded = loadedK1Classes() - loadedInitially
        assertTrue(loaded.isEmpty(), "${codeUnderTest.name} must not use the K1 frontend, but these K1 classes are loaded: $loaded")
    }
}

abstract class AbstractCustomScriptDefinitionConfigurationRefinementTest : AbstractScriptConfigurationRefinementTest() {
    override val configurator: AnalysisApiTestConfigurator =
        AnalysisApiFirCustomScriptDefinitionTestConfigurator(analyseInDependentSession = false)
}
