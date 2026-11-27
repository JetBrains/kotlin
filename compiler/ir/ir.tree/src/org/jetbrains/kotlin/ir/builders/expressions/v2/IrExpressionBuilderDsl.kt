/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("IrExpressionBuilders")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ir.builders.expressions.v2

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.getRealOwner
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.util.allTypeParameters
import org.jetbrains.kotlin.ir.util.parentAsClass
import java.util.Collections

@DslMarker
annotation class IrExpressionBuilderDsl

internal fun initializeShape(builder: IrCallBuilder) {
    val callee = builder.symbol.getRealOwner()
    builder.initializeShape(callee.parameters.size, callee.typeParameters.size)
}

internal fun initializeShape(builder: IrConstructorCallBuilder) {
    val constructor = builder.symbol.owner
    builder.constructorTypeArgumentsCount = constructor.typeParameters.size
    builder.initializeShape(constructor.parameters.size, constructor.parentAsClass.typeParameters.size + constructor.typeParameters.size)
}

internal fun initializeShape(builder: IrDelegatingConstructorCallBuilder) {
    val constructor = builder.symbol.owner
    builder.initializeShape(constructor.parameters.size, constructor.allTypeParameters.size)
}

internal fun initializeShape(builder: IrEnumConstructorCallBuilder) {
    val constructor = builder.symbol.owner
    builder.initializeShape(constructor.parameters.size, constructor.allTypeParameters.size)
}

internal fun initializeShape(builder: IrFunctionReferenceBuilder) {
    val callee = builder.symbol.getRealOwner()
    builder.initializeShape(callee.parameters.size, callee.typeParameters.size)
}

private fun IrMemberAccessExpressionBuilder<*>.initializeShape(argumentsCount: Int, typeArgumentsCount: Int) {
    if (argumentsCount > 0) arguments.addAll(Collections.nCopies(argumentsCount, null))
    if (typeArgumentsCount > 0) typeArguments.addAll(Collections.nCopies(typeArgumentsCount, null))
}

/**
 * The shape of the arguments of the node is taken from the callee. The arguments of the builder must have the same shape,
 * unless they are empty (e.g. for a property reference, whose shape depends on its accessors).
 */
internal fun initializeArguments(builder: IrMemberAccessExpressionBuilder<*>, node: IrMemberAccessExpression<*>) {
    node.initializeTargetShapeFromSymbol()
    node.initializeEmptyTypeArguments(builder.typeArguments.size)
    if (builder.arguments.isNotEmpty()) {
        check(builder.arguments.size == node.arguments.size) {
            "Arguments of a call to ${builder.symbol} don't match the parameters of the callee: " +
                    "${builder.arguments.size} instead of ${node.arguments.size}"
        }
        builder.arguments.forEachIndexed { index, argument -> node.arguments[index] = argument }
    }
    builder.typeArguments.forEachIndexed { index, typeArgument -> node.typeArguments[index] = typeArgument }
}

internal fun applyDefaults(builder: IrConstantPrimitiveBuilder) {
    if (!builder.isTypeInitialized) builder.type = builder.value.type
}

internal fun IrBuilderWithScope.setReturnTargetFromScope(builder: IrReturnBuilder) {
    (scope.scopeOwnerSymbol as? IrReturnTargetSymbol)?.let { builder.returnTargetSymbol = it }
}
