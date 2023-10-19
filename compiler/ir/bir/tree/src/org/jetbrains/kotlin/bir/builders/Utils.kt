/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.builders

import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.*
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.parentAsClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.name.Name

fun BirCall.setCall(callee: BirSimpleFunction) {
    this.symbol = callee
    type = callee.returnType
    repeat(callee.valueParameters.size - valueArguments.size) {
        valueArguments += null
    }
}


fun BirConstructorCall.setCall(callee: BirConstructor, constructedClass: BirClass = callee.owner.parentAsClass) {
    this.symbol = callee
    type = callee.returnType
    repeat(callee.valueParameters.size - valueArguments.size) {
        valueArguments += null
    }
    constructorTypeArgumentsCount = callee.owner.typeParameters.size

    val missingTypeArguments = callee.owner.typeParameters.size + constructedClass.typeParameters.size - typeArguments.size
    if (missingTypeArguments > 0) {
        typeArguments += List<BirType?>(missingTypeArguments) { null }
    }
}

fun BirVariable.setTemporary(nameHint: String? = null) {
    origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
    name = Name.identifier(nameHint ?: "tmp")
}

fun BirWhen.addIfThenElse(`if`: () -> BirBranch, `else`: () -> BirElseBranch) {
    branches.add(`if`())
    branches.add(`else`())
}


fun BirAttributeContainer.copyAttributes(other: BirAttributeContainer) {
    attributeOwnerId = other.attributeOwnerId
    //originalBeforeInline = other.originalBeforeInline
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