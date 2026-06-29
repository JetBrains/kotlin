/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class KtSimpleNameExpressionTest : KotlinTestWithEnvironment() {
    @Test
    fun testGetReceiverExpressionIdentifier() {
        // Binary Expressions
        assertReceiver("1 + 2", "1")
        assertReceiver("1 in array(1)", "array(1)")
        assertReceiver("1 !in array(1)", "array(1)")
        assertReceiver("1 to 2", "1")
    }

    private fun assertReceiver(exprString: String, expected: String) {
        val expression = KtPsiFactory(project).createExpression(exprString) as KtBinaryExpression
        Assertions.assertEquals(expected, expression.operationReference.getReceiverExpression()!!.text)
    }
}
