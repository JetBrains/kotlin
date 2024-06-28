/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OperatorNamesToSymbolsTest {
    @Test
    fun testAllOperatorsMapped() {
        val allEntries = OperatorNameConventions.TOKENS_BY_OPERATOR_NAME.toMutableMap()
        val mapName = "${OperatorNameConventions::class.java.simpleName}.${OperatorNameConventions::TOKENS_BY_OPERATOR_NAME.name}"

        fun check(token: KtSingleValueToken, name: Name) {
            val mappedToken = allEntries.remove(name)
            assertNotNull(mappedToken, "'$name' not in $mapName")
            assertEquals(token.value, mappedToken, "Token not matching")
        }

        for ((token, name) in OperatorConventions.UNARY_OPERATION_NAMES) {
            check(token, name)
        }

        for ((token, name) in OperatorConventions.BINARY_OPERATION_NAMES) {
            check(token, name)
        }

        assertTrue(
            allEntries.isEmpty(),
            "Entries in $mapName that were not in ${OperatorConventions::class.java.simpleName}: ${allEntries.entries.joinToString()}"
        )
    }
}
