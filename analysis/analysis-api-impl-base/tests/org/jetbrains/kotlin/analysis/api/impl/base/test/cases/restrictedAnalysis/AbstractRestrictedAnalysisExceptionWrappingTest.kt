/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.restrictedAnalysis

import com.intellij.openapi.project.IndexNotReadyException
import org.jetbrains.kotlin.analysis.api.platform.restrictedAnalysis.KaRestrictedAnalysisException
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * Checks that exceptions are correctly wrapped in [KaRestrictedAnalysisException] (or not, in some cases) during restricted analysis mode.
 *
 * The test data should contain a `THROW` directive with the fully qualified name of the exception or error to throw. This error must have
 * a parameterless or single-parameter `Throwable` constructor.
 */
abstract class AbstractRestrictedAnalysisExceptionWrappingTest : AbstractRestrictedAnalysisTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val throwableFqName = mainModule.testModule.directives[Directives.THROW].singleOrNull()
            ?: error("An exception to throw should be specified with the `THROW` directive..")

        testAnalysisRestricted(throwableFqName, mainFile, mainModule, testServices)
        testAnalysisUnrestricted(throwableFqName, mainFile, mainModule, testServices)
    }

    private fun testAnalysisRestricted(
        throwableFqName: String,
        mainFile: KtFile,
        mainModule: KtTestModule,
        testServices: TestServices,
    ) {
        mainModule.restrictedAnalysisService.enableRestrictedAnalysisMode = true

        val shouldBeWrapped = !mainModule.testModule.directives.contains(Directives.EXPECT_UNWRAPPED)

        playCatch(throwableFqName, mainFile) { throwable, caughtThrowable ->
            if (shouldBeWrapped) {
                testServices.assertions.assertTrue(caughtThrowable is KaRestrictedAnalysisException) {
                    "Expected a `${KaRestrictedAnalysisException::class.simpleName}` to be caught, but caught" +
                            " `${caughtThrowable::class.simpleName}` instead."
                }
                testServices.assertions.assertEquals(throwable, caughtThrowable.cause) {
                    "The cause of the `${KaRestrictedAnalysisException::class.simpleName}` should be the wrapped exception, but got" +
                            " ${caughtThrowable.cause} instead."
                }
            } else {
                testServices.assertions.assertEquals(throwable, caughtThrowable) {
                    "Expected the same throwable to be caught as-is even inside restricted analysis mode (`EXPECT_UNWRAPPED`), but caught" +
                            " `${caughtThrowable::class.simpleName}` instead."
                }
            }
        }
    }

    /**
     * Outside restricted analysis mode, all exceptions and errors should be rethrown as-is.
     */
    private fun testAnalysisUnrestricted(
        throwableFqName: String,
        mainFile: KtFile,
        mainModule: KtTestModule,
        testServices: TestServices,
    ) {
        mainModule.restrictedAnalysisService.enableRestrictedAnalysisMode = false

        playCatch(throwableFqName, mainFile) { throwable, caughtThrowable ->
            testServices.assertions.assertEquals(throwable, caughtThrowable) {
                "Expected the same throwable to be caught as-is outside restricted analysis mode, but caught" +
                        " `${caughtThrowable::class.simpleName}` instead."
            }
        }
    }

    private inline fun playCatch(throwableFqName: String, mainFile: KtFile, action: (Throwable, Throwable) -> Unit) {
        val throwable = instantiateThrowable(throwableFqName)
        try {
            analyseForTest(mainFile) {
                throw throwable
            }
        } catch (caughtThrowable: Throwable) {
            action(throwable, caughtThrowable)
        }
    }

    private fun instantiateThrowable(throwableFqName: String): Throwable {
        // `IndexNotReadyException` has no public constructors. :)
        if (throwableFqName == IndexNotReadyException::class.qualifiedName) {
            return IndexNotReadyException.create()
        }

        val exceptionClass = Class.forName(throwableFqName)
        val defaultConstructor = exceptionClass.constructors.singleOrNull { it.parameterCount == 0 }

        val instance = if (defaultConstructor != null) {
            exceptionClass.newInstance()
        } else {
            val throwableConstructor = exceptionClass.constructors
                .singleOrNull { it.parameterCount == 1 && it.parameterTypes[0] == Throwable::class.java }
                ?: error("The specified throwable `$throwableFqName` should have an empty or single-parameter `Throwable` constructor.")

            throwableConstructor.newInstance(Throwable("Mock cause"))
        }

        return instance as? Throwable ?: error("The specified class `$throwableFqName` is not a throwable.")
    }

    private object Directives : SimpleDirectivesContainer() {
        val THROW by stringDirective("The fully qualified name of the exception or error to throw.")
        val EXPECT_UNWRAPPED by directive(
            "Specified when the resulting exception should NOT be wrapped in `KaRestrictedAnalysisException`.",
        )
    }
}
