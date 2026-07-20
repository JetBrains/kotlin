/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.utils

import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrConversionScope
import org.jetbrains.kotlin.fir.backend.Fir2IrVisitor
import org.jetbrains.kotlin.fir.backend.generators.CallAndReferenceGenerator
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.toConstKind
import org.jetbrains.kotlin.ir.declarations.createExpressionBody
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.removeAnnotations
import org.jetbrains.kotlin.types.ConstantValueKind

fun FirLiteralExpression.getIrConstKind(): IrConstKind = when (kind) {
    ConstantValueKind.IntegerLiteral, ConstantValueKind.UnsignedIntegerLiteral -> {
        val type = resolvedType as ConeIntegerLiteralType
        type.getApproximatedType().toConstKind()!!.toIrConstKind()
    }

    else -> kind.toIrConstKind()
}

fun FirLiteralExpression.toIrConst(irType: IrType): IrConst {
    return convertWithOffsets { startOffset, endOffset ->
        val kind = getIrConstKind()
        // Strip all annotations (including special annotations such as @EnhancedNullability) from a constant type
        val type = irType.removeAnnotations()
        when (kind) {
            IrConstKind.Boolean -> IrConstImpl.boolean(startOffset, endOffset, type, value as Boolean)
            IrConstKind.Byte -> IrConstImpl.byte(startOffset, endOffset, type, (value as Number).toByte())
            IrConstKind.Short -> IrConstImpl.short(startOffset, endOffset, type, (value as Number).toShort())
            IrConstKind.Int -> IrConstImpl.int(startOffset, endOffset, type, (value as Number).toInt())
            IrConstKind.Long -> IrConstImpl.long(startOffset, endOffset, type, (value as Number).toLong())
            IrConstKind.Char -> IrConstImpl.char(startOffset, endOffset, type, value as Char)
            IrConstKind.Float -> IrConstImpl.float(startOffset, endOffset, type, (value as Number).toFloat())
            IrConstKind.Double -> IrConstImpl.double(startOffset, endOffset, type, (value as Number).toDouble())
            IrConstKind.String -> IrConstImpl.string(startOffset, endOffset, type, value as String)
            IrConstKind.Null -> IrConstImpl.constNull(startOffset, endOffset, type)
        }
    }
}

private fun ConstantValueKind.toIrConstKind(): IrConstKind = when (this) {
    ConstantValueKind.Null -> IrConstKind.Null
    ConstantValueKind.Boolean -> IrConstKind.Boolean
    ConstantValueKind.Char -> IrConstKind.Char

    ConstantValueKind.Byte -> IrConstKind.Byte
    ConstantValueKind.Short -> IrConstKind.Short
    ConstantValueKind.Int -> IrConstKind.Int
    ConstantValueKind.Long -> IrConstKind.Long

    ConstantValueKind.UnsignedByte -> IrConstKind.Byte
    ConstantValueKind.UnsignedShort -> IrConstKind.Short
    ConstantValueKind.UnsignedInt -> IrConstKind.Int
    ConstantValueKind.UnsignedLong -> IrConstKind.Long

    ConstantValueKind.String -> IrConstKind.String
    ConstantValueKind.Float -> IrConstKind.Float
    ConstantValueKind.Double -> IrConstKind.Double
    ConstantValueKind.IntegerLiteral, ConstantValueKind.UnsignedIntegerLiteral -> throw IllegalArgumentException()
    ConstantValueKind.Error -> throw IllegalArgumentException()
}

/**
 * This method is intended to be used for default values of annotation parameters (compile-time strings, numbers, enum values, KClasses)
 * where they are needed and may produce incorrect results for values that may be encountered outside annotations.
 *
 *
 * [CallAndReferenceGenerator] relies on the [Fir2IrVisitor.annotationMode] to properly generate the arguments of call, and it's essential
 * to pass `annotationMode = true` to it.
 *
 * But the problem is that [components] contain the main instance of [CallAndReferenceGenerator], which contains not the visitor
 * created in this function, but the main visitor.
 *
 * So to properly handle this situation, it's required to create a new [CallAndReferenceGenerator] which will store the proper visitor.
 */
context(components: Fir2IrComponents)
fun FirExpression.asCompileTimeIrInitializerForAnnotationParameter(
    expectedTypeForAnnotationArgument: ConeKotlinType? = null,
): IrExpressionBody {
    val componentsWithReplacedCallGenerator = object : Fir2IrComponents by components {
        override val callGenerator: CallAndReferenceGenerator
            get() = _callGenerator!!

        var _callGenerator: CallAndReferenceGenerator? = null
    }
    val conversionScope = Fir2IrConversionScope(components.configuration)
    val visitor = Fir2IrVisitor(componentsWithReplacedCallGenerator, conversionScope)
    componentsWithReplacedCallGenerator._callGenerator = CallAndReferenceGenerator(
        componentsWithReplacedCallGenerator,
        visitor,
        conversionScope
    )
    val expression = visitor.withAnnotationMode {
        visitor.convertToIrExpression(
            this,
            expectedType = expectedTypeForAnnotationArgument,
        )
    }
    return IrFactoryImpl.createExpressionBody(expression)
}
