/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.nextgen

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument

class DeclarationBaseCarrier(
    val symbolId: Int,
    val origin: IrDeclarationOrigin,
    val coordinates: CoordinatesCarrier,
    val annotations: List<IrConstructorCall>
)

class FunctionBaseCarrier(
    val declarationBaseCarrier: DeclarationBaseCarrier,
    val nameId: Int,
    val visibility: Visibility,
    val isInline: Boolean,
    val isExternal: Boolean,
    val typeParameters: List<IrTypeParameter>,
    val dispathReceiver: IrValueParameter?,
    val extensionReceiver: IrValueParameter?,
    val valueParameters: List<IrValueParameter>,
    val bodyIndex: Int?,
    val returnTypeIndex: Int
)

class MemberAccessCarrier(
    val dispatchReceiver: IrExpression?,
    val extensionReceiver: IrExpression?,
    val valueArguments: List<IrExpression>,
    val typeArguments: List<IrType>
)

class FieldAccessCarrier(val symbolId: Int, val superId: Int?, val receiver: IrExpression?)

class LoopCarrier(val loopId: Int, val condition: IrExpression, val label: Int?, val body: IrExpression?, val origin: IrStatementOrigin?)

class CoordinatesCarrier(val start: Int, val end: Int)

class NullableExpression(val expression: IrExpression?)
