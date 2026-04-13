/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.junit.Assert

class KtSimpleNameExpressionTest : KotlinTestWithEnvironment() {
    fun testGetReceiverExpressionIdentifier() {
        // Binary Expressions
        assertReceiver("1 + 2", "1")
        assertReceiver("1 in array(1)", "array(1)")
        assertReceiver("1 !in array(1)", "array(1)")
        assertReceiver("1 to 2", "1")
    }

    private fun assertReceiver(exprString: String, expected: String) {
        val expression = KtPsiFactory(project).createExpression(exprString) as KtBinaryExpression
        Assert.assertEquals(expected, expression.operationReference.getReceiverExpression()!!.text)
    }

    @OptIn(K1Deprecation::class)
    override fun createEnvironment(): KotlinCoreEnvironment {
        return KotlinCoreEnvironment.createForTests(
            testRootDisposable, KotlinTestUtils.newConfiguration(), EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
    }
}
