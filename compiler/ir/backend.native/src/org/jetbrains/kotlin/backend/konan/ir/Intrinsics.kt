/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.konan.IntrinsicType
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.getAnnotationStringValue
import org.jetbrains.kotlin.ir.util.hasAnnotation

fun tryGetIntrinsicType(callSite: IrFunctionAccessExpression): IntrinsicType? =
    tryGetIntrinsicType(callSite.symbol.owner)

fun tryGetIntrinsicType(function: IrFunction): IntrinsicType? =
    if (function.isTypedIntrinsic) getIntrinsicType(function) else null

fun getIntrinsicType(callSite: IrFunctionAccessExpression) = getIntrinsicType(callSite.symbol.owner)

private fun getIntrinsicType(function: IrFunction): IntrinsicType {
    val annotation = function.annotations.findAnnotation(RuntimeNames.typedIntrinsicAnnotation)!!
    val value = annotation.getAnnotationStringValue()!!
    return IntrinsicType.valueOf(value)
}

val IrFunction.isTypedIntrinsic: Boolean
    get() = annotations.hasAnnotation(KonanFqNames.typedIntrinsic)