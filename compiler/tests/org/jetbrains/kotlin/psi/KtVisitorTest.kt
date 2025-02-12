/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment

class KtVisitorTest : KotlinTestWithEnvironment() {
    fun testTopLevelFunctionContextVisiting() {
        doTestContextReceiverVisiting("""context(String, Int) fun foo() {}""")
    }

    fun testMemberFunctionContextVisiting() {
        doTestContextReceiverVisiting("""class Foo { context(String, Int) fun foo() {} }""")
    }

    fun testClassContextVisiting() {
        doTestContextReceiverVisiting("""context(String, Int) class Foo""")
    }

    fun testFunctionalParameterContextVisiting() {
        doTestContextReceiverVisiting("""fun foo(fn: context(String, Int) () -> Unit) {}""")
    }

    private fun doTestContextReceiverVisiting(code: String) {
        val ktElement = KtPsiFactory(project).createFile(code)
        var visited = false
        ktElement.accept(object : KtTreeVisitorVoid() {
            override fun visitContextReceiverList(contextReceiverList: KtContextReceiverList) {
                visited = true
            }
        })

        assert(visited) {
            """
            Context receiver list was not visited:

            $code
            
            """.trimIndent()
        }
    }

    override fun createEnvironment(): KotlinCoreEnvironment? {
        return KotlinCoreEnvironment.createForTests(
            testRootDisposable, KotlinTestUtils.newConfiguration(), EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
    }
}
