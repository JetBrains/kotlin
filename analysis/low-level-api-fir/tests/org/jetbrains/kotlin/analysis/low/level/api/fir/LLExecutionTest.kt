/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

@ExtendWith(LLGenericAnalysisTestExtension::class)
abstract class LLExecutionTest(val testDirPathString: String) : AbstractAnalysisApiBasedTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    fun performTest(path: String, block: (TestServices, KtFile?, KtTestModule) -> Unit) {
        runTest(path) { testServices ->
            val (mainFile, mainModule) = findMainFileAndModule(testServices)
            block(testServices, mainFile, mainModule)
        }
    }
}

private class LLGenericAnalysisTestExtension : BeforeTestExecutionCallback, AfterTestExecutionCallback, ParameterResolver {
    private companion object {
        private val SUPPORTED_PARAMETER_TYPES = listOf(
            TestServices::class.java,
            KtFile::class.java,
            KtTestModule::class.java,
        )
    }

    private class State(val testServices: TestServices, val mainFile: KtFile?, val mainModule: KtTestModule)

    private var cachedState = ThreadLocal<State>()

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        val parameterType = parameterContext.parameter.type
        return SUPPORTED_PARAMETER_TYPES.any { it.isAssignableFrom(parameterType) }
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any? {
        val cachedState = this.cachedState.get() ?: error("State is not cached yet")
        val parameterType = parameterContext.parameter.type
        return when {
            parameterType.isAssignableFrom(TestServices::class.java) -> cachedState.testServices
            parameterType.isAssignableFrom(KtFile::class.java) -> cachedState.mainFile
            parameterType.isAssignableFrom(KtTestModule::class.java) -> cachedState.mainModule
            else -> error("Unsupported parameter type $parameterType")
        }
    }

    override fun beforeTestExecution(context: ExtensionContext) {
        val testInstance = context.requiredTestInstance as LLExecutionTest
        val testFilePath = getTestFilePath(testInstance.testDirPathString, context.requiredTestMethod.name)

        testInstance.performTest(testFilePath.toString()) { testServices, mainFile, mainModule ->
            require(cachedState.get() == null)
            cachedState.set(State(testServices, mainFile, mainModule))
        }
    }

    override fun afterTestExecution(context: ExtensionContext?) {
        cachedState.remove()
    }

    private fun getTestFilePath(testDirPathString: String, testFileName: String): Path {
        return Paths.get(testDirPathString, "$testFileName.kt").takeIf { it.exists() }
            ?: Paths.get(testDirPathString, "$testFileName.kts").takeIf { it.exists() }
            ?: error("Cannot find test file $testFileName.kt(s) in $testDirPathString")
    }
}
