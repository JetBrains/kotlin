/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import com.intellij.psi.StubBasedPsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildConstExpression
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.stubs.KotlinConstantExpressionStub
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderWithTextStub
import org.jetbrains.kotlin.types.ConstantValueKind

open class StubBasedFirConstDeserializer(
    val session: FirSession
) {
    protected val constantCache = mutableMapOf<CallableId, FirExpression>()

    open fun loadConstant(property: KtProperty, callableId: CallableId): FirExpression? {
        if (!property.hasModifier(KtTokens.CONST_KEYWORD)) return null
        constantCache[callableId]?.let { return it }
        val initializer = property.initializer as? StubBasedPsiElement<*> ?: return null
        val stub = initializer.stub ?: return null
        if (stub is KotlinConstantExpressionStub) {
            val text = stub.value()
            return buildFirConstant(text, stub.kind()).also { constantCache[callableId] = it }
        }
        if (stub is KtStringTemplateExpression) {
            val textStub = stub.entries[0].stub as KotlinPlaceHolderWithTextStub<*>
            return buildConstExpression(null, ConstantValueKind.String, textStub.text()).also { constantCache[callableId] = it }
        }
        return null
    }
}

fun buildFirConstant(
    initializer: String, constKind: org.jetbrains.kotlin.psi.stubs.ConstantValueKind
): FirExpression {
    return when (constKind) {
        org.jetbrains.kotlin.psi.stubs.ConstantValueKind.BOOLEAN_CONSTANT -> buildConstExpression(
            null,
            ConstantValueKind.Boolean,
            java.lang.Boolean.parseBoolean(initializer)
        )
        org.jetbrains.kotlin.psi.stubs.ConstantValueKind.FLOAT_CONSTANT -> buildConstExpression(
            null,
            ConstantValueKind.Double,
            java.lang.Double.parseDouble(initializer)
        )
        org.jetbrains.kotlin.psi.stubs.ConstantValueKind.CHARACTER_CONSTANT -> buildConstExpression(
            null,
            ConstantValueKind.Char,
            initializer.toCharArray()[0]
        )
        org.jetbrains.kotlin.psi.stubs.ConstantValueKind.INTEGER_CONSTANT -> buildConstExpression(
            null,
            ConstantValueKind.Long,
            java.lang.Long.parseLong(initializer)
        )
        org.jetbrains.kotlin.psi.stubs.ConstantValueKind.NULL -> buildConstExpression(
            null,
            ConstantValueKind.Null,
            null
        )
    }
}