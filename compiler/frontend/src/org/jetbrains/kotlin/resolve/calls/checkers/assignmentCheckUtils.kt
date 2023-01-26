/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.types.KotlinType

fun isAssignmentCorrectWithDataFlowInfo(
    leftType: KotlinType,
    rightExpression: KtExpression,
    rightType: KotlinType,
    context: CallCheckerContext
): Boolean {
    val kotlinTypeChecker = context.callComponents.kotlinTypeChecker
    if (kotlinTypeChecker.isSubtypeOf(rightType, leftType)) return true
    val dfi = context.dataFlowInfo
    val dfvFactory = context.dataFlowValueFactory
    val stableTypesFromDataFlow = dfi.getStableTypes(
        dfvFactory.createDataFlowValue(rightExpression, rightType, context.trace.bindingContext, context.moduleDescriptor),
        context.languageVersionSettings
    )
    return stableTypesFromDataFlow.any { kotlinTypeChecker.isSubtypeOf(it, leftType) }
}