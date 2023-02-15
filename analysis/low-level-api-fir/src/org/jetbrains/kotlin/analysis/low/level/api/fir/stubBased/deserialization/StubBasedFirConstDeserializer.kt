/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildConstExpression
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.types.ConstantValueKind

//todo jvm constants
open class FirConstDeserializer(
    val session: FirSession
) {
    protected val constantCache = mutableMapOf<CallableId, FirExpression>()

    open fun loadConstant(property: KtProperty, callableId: CallableId): FirExpression? {
        if (!property.hasModifier(KtTokens.CONST_KEYWORD)) return null
        constantCache[callableId]?.let { return it }
        val initializer = property.initializer
        require(initializer != null)
        val text = initializer.text //todo get constant initializer
        return buildFirConstant(text, null, text)?.also { constantCache[callableId] = it }
    }
}

fun buildFirConstant(
    initializer: Any?, sourceValue: Any?, constKind: String
): FirExpression? {
    return when (constKind) {
        "BYTE", "B" -> buildConstExpression(null, ConstantValueKind.Byte, ((initializer ?: sourceValue) as Number).toByte())
        "CHAR", "C" -> buildConstExpression(null, ConstantValueKind.Char, ((initializer ?: sourceValue) as Number).toInt().toChar())
        "SHORT", "S" -> buildConstExpression(null, ConstantValueKind.Short, ((initializer ?: sourceValue) as Number).toShort())
        "INT", "I" -> buildConstExpression(null, ConstantValueKind.Int, (initializer ?: sourceValue) as Int)
        "LONG", "J" -> buildConstExpression(null, ConstantValueKind.Long, (initializer ?: sourceValue) as Long)
        "FLOAT", "F" -> buildConstExpression(null, ConstantValueKind.Float, (initializer ?: sourceValue) as Float)
        "DOUBLE", "D" -> buildConstExpression(null, ConstantValueKind.Double, (initializer ?: sourceValue) as Double)
        "BOOLEAN", "Z" -> buildConstExpression(null, ConstantValueKind.Boolean, (initializer ?: sourceValue) != 0)
        "STRING", "Ljava/lang/String;" -> buildConstExpression(
            null, ConstantValueKind.String, (initializer ?: sourceValue) as String
        )
        else -> null
    }
}

fun CallableId.replaceName(newName: Name): CallableId {
    return CallableId(this.packageName, this.className, newName)
}
