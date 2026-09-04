/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.utils

import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.junit.jupiter.api.Test
import kotlin.reflect.KProperty
import kotlin.test.assertEquals

class PsiTokenUtilsTest {
    @Suppress("UnstableApiUsage")
    @Test
    fun testElementTypeKmpIds() {
        val psiFields = KtNodeTypes::class.java.declaredFields
        val kmpFields = org.jetbrains.kotlin.kmp.parser.KtNodeTypes::class.members.filterIsInstance<KProperty<*>>().filter { !it.isConst }

        psiFields.forEach { psiField ->
            val psiNodeType = psiField.get(null) as IElementType
            val psiId = psiNodeType.kmpId()
            if (psiField.isAnnotationPresent(java.lang.Deprecated::class.java)) return@forEach

            val kmpField = kmpFields.find { it.name == psiField.name }
                ?: error("PSI_NODE_TYPE = $psiNodeType not found in KMP")

            val kmpNodeType = kmpField.getter.call(org.jetbrains.kotlin.kmp.parser.KtNodeTypes) as SyntaxElementType
            val kmpId = org.jetbrains.kotlin.kmp.parser.KtNodeTypes.getElementTypeId(kmpNodeType)

            assertEquals(
                psiId, kmpId,
                "KMP_ID = $kmpId KMP_NODE_TYPE = $kmpNodeType PSI_ID = $psiId PSI_NODE_TYPE = $psiNodeType"
            )
        }
    }

    @Suppress("UnstableApiUsage")
    @Test
    fun testTokenKmpIds() {
        val psiFields = KtTokens::class.java.declaredFields.filter { !it.type.isPrimitive }
        val kmpFields = org.jetbrains.kotlin.kmp.lexer.KtTokens::class.members.filterIsInstance<KProperty<*>>().filter { !it.isConst }

        psiFields.forEach { psiField ->
            val psiNodeType = psiField.get(null) as? KtToken ?: return@forEach
            val psiId = psiNodeType.kmpId()
            if (psiField.isAnnotationPresent(java.lang.Deprecated::class.java)) return@forEach

            val kmpField = kmpFields.find { it.name == psiField.name || it.name.replace("_MODIFIER", "_KEYWORD") == psiField.name }
                ?: error("PSI_NODE_TYPE = $psiNodeType (${psiField.name}) not found in KMP")

            val kmpNodeType = kmpField.getter.call(org.jetbrains.kotlin.kmp.lexer.KtTokens) as SyntaxElementType
            val kmpId = org.jetbrains.kotlin.kmp.lexer.KtTokens.getElementTypeId(kmpNodeType)

            assertEquals(
                psiId, kmpId,
                "KMP_ID = $kmpId KMP_NODE_TYPE = $kmpNodeType PSI_ID = $psiId PSI_NODE_TYPE = $psiNodeType"
            )
        }
    }
}
