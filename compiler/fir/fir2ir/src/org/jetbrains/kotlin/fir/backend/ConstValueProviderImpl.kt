/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildConstExpression
import org.jetbrains.kotlin.fir.serialization.constant.ConstValueProvider
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.types.ConstantValueKind

class ConstValueProviderImpl(
    private val components: Fir2IrComponents,
) : ConstValueProvider() {
    override val session: FirSession = components.session

    override fun getConstantValueForProperty(firProperty: FirProperty): FirConstExpression<*>? {
        val irProperty: IrProperty = components.declarationStorage.getCachedIrProperty(firProperty) ?: return null
        if (!irProperty.isConst) return null
        val irConst = irProperty.backingField?.initializer?.expression as? IrConst<*> ?: return null
        return irConst.toFirConst()
    }

    private fun IrConst<*>.getConstantKind(): ConstantValueKind<*>? {
        if (this.kind == IrConstKind.Null) return ConstantValueKind.Null

        val constType = this.type.makeNotNull().removeAnnotations()
        return when (this.type.getPrimitiveType()) {
            PrimitiveType.BOOLEAN -> ConstantValueKind.Boolean
            PrimitiveType.CHAR -> ConstantValueKind.Char
            PrimitiveType.BYTE -> ConstantValueKind.Byte
            PrimitiveType.SHORT -> ConstantValueKind.Short
            PrimitiveType.INT -> ConstantValueKind.Int
            PrimitiveType.LONG -> ConstantValueKind.Long
            PrimitiveType.FLOAT -> ConstantValueKind.Float
            PrimitiveType.DOUBLE -> ConstantValueKind.Double
            null -> when (constType.getUnsignedType()) {
                UnsignedType.UBYTE -> ConstantValueKind.UnsignedByte
                UnsignedType.USHORT -> ConstantValueKind.UnsignedShort
                UnsignedType.UINT -> ConstantValueKind.UnsignedInt
                UnsignedType.ULONG -> ConstantValueKind.UnsignedLong
                null -> when {
                    constType.isString() -> ConstantValueKind.String
                    else -> null
                }
            }
        }
    }

    private fun <T> IrConst<T>.toFirConst(): FirConstExpression<T>? {
        @Suppress("UNCHECKED_CAST")
        val kind = getConstantKind() as? ConstantValueKind<T> ?: return null
        return buildConstExpression(null, kind, this.value)
    }
}