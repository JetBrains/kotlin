/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.element.SyntaxTokenTypes
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.junit.jupiter.api.Test
import java.lang.Deprecated
import java.lang.reflect.Modifier
import kotlin.reflect.KProperty
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(KtImplementationDetail::class)
@Suppress("UnstableApiUsage")
class PsiKmpSyntaxIdsConsistencyTest {
    @Test
    fun nodeTypes() {
        val psiFields = KtNodeTypes::class.java.declaredFields
        val kmpFields = org.jetbrains.kotlin.kmp.parser.KtNodeTypes::class.members.filterIsInstance<KProperty<*>>().filter { !it.isConst }

        psiFields.forEach { psiField ->
            val psiNodeType = psiField.get(null) as IElementType
            val psiId = psiNodeType.syntaxId()
            if (psiField.isAnnotationPresent(Deprecated::class.java)) return@forEach

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

    @Test
    fun tokens() {
        val psiFields = KtTokens::class.java.declaredFields.filter { !it.type.isPrimitive }
        val kmpFields = org.jetbrains.kotlin.kmp.lexer.KtTokens::class.members.filterIsInstance<KProperty<*>>().filter { !it.isConst }

        psiFields.forEach { psiField ->
            val psiNodeType = psiField.get(null) as? KtToken ?: return@forEach
            val psiId = psiNodeType.syntaxId()
            if (psiField.isAnnotationPresent(Deprecated::class.java)) return@forEach

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

    @Test
    fun basicElementTypes() {
        listOf(
            TokenType.WHITE_SPACE to SyntaxTokenTypes.WHITE_SPACE,
            TokenType.BAD_CHARACTER to SyntaxTokenTypes.BAD_CHARACTER,
            TokenType.ERROR_ELEMENT to SyntaxTokenTypes.ERROR_ELEMENT,
        ).forEach { [psiElementType, kmpElementType] ->
            val psiId = psiElementType.syntaxId()
            val kmpId = org.jetbrains.kotlin.kmp.lexer.BasicElementTypes.getElementTypeId(kmpElementType)

            assertNotEquals(NO_ID, psiId, "PSI_ELEMENT_TYPE = $psiElementType has no ID")
            assertEquals(
                psiId, kmpId,
                "KMP_ID = $kmpId KMP_ELEMENT_TYPE = $kmpElementType PSI_ID = $psiId PSI_ELEMENT_TYPE = $psiElementType"
            )
        }
    }

    @Test
    fun elementTypeIsInverseOfSyntaxId() {
        assertNull(NO_ID.elementType())

        // KDoc element types are not mapped by `syntaxId()`
        for (holder in listOf(BasicSyntaxElementTypeIds::class, KotlinSyntaxTokenIds::class, KotlinSyntaxNodeTypeIds::class)) {
            val fields = holder.java.declaredFields.filter { Modifier.isStatic(it.modifiers) && it.type == Int::class.javaPrimitiveType }
            for (field in fields) {
                val id = field.getInt(null)
                val elementType = assertNotNull(id.elementType(), "No element type for ${holder.simpleName}.${field.name} = $id")
                assertEquals(id, elementType.syntaxId(), "${holder.simpleName}.${field.name} is mapped to $elementType with another ID")
            }
        }
    }

    @Test
    fun modifierElementTypeNames() {
        assertEquals("public", KotlinSyntaxTokenIds.PUBLIC_MODIFIER.elementType().toString())
        assertEquals("fun", KotlinSyntaxTokenIds.FUN_MODIFIER.elementType().toString())
        assertEquals("in", KotlinSyntaxTokenIds.IN_MODIFIER.elementType().toString())
    }
}
