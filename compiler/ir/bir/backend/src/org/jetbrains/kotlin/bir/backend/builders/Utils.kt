/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.builders

import org.jetbrains.kotlin.bir.backend.utils.listOfNulls
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.*
import org.jetbrains.kotlin.bir.symbols.BirConstructorSymbol
import org.jetbrains.kotlin.bir.symbols.BirSimpleFunctionSymbol
import org.jetbrains.kotlin.bir.util.parentAsClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.name.Name

fun BirCall.setCall(callee: BirSimpleFunction) {
    this.symbol = callee.symbol
    type = callee.returnType
    valueArguments += listOfNulls(callee.valueParameters.size - valueArguments.size)
    typeArguments += listOfNulls(callee.typeParameters.size - typeArguments.size)
}

fun BirConstructorCall.setCall(callee: BirConstructor, constructedClass: BirClass = callee.parentAsClass) {
    this.symbol = callee.symbol
    type = callee.returnType
    valueArguments += listOfNulls(callee.valueParameters.size - valueArguments.size)
    typeArguments += listOfNulls(callee.typeParameters.size + constructedClass.typeParameters.size - typeArguments.size)
    constructorTypeArgumentsCount = callee.typeParameters.size
}

fun BirVariable.setTemporary(nameHint: String? = null) {
    origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
    name = Name.identifier(nameHint ?: "tmp")
}

fun BirWhen.addIfThenElse(`if`: () -> BirBranch, `else`: () -> BirElseBranch) {
    branches.add(`if`())
    branches.add(`else`())
}


fun BirSimpleFunction.copyFlagsFrom(from: BirSimpleFunction) {
    isInline = from.isInline
    isInfix = from.isInfix
    isExpect = from.isExpect
    isSuspend = from.isSuspend
    isOperator = from.isOperator
    isTailrec = from.isTailrec
    isFakeOverride = from.isFakeOverride
    isExternal = from.isExternal
}

fun BirConstructor.copyFlagsFrom(from: BirConstructor) {
    isInline = from.isInline
    isExpect = from.isExpect
    isExternal = from.isExternal
    isPrimary = from.isPrimary
}