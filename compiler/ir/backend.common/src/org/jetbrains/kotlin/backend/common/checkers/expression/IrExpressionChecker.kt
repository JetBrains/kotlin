/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.expression

import org.jetbrains.kotlin.backend.common.checkers.IrElementChecker
import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol

internal fun <E : IrExpression> List<IrElementChecker<E>>.check(expression: E, context: CheckerContext) {
    for (checker in this) {
        checker.check(expression, context)
    }
}

internal typealias IrVarargChecker = IrElementChecker<IrVararg>
internal typealias IrFieldAccessChecker = IrElementChecker<IrFieldAccessExpression>
internal typealias IrDeclarationReferenceChecker = IrElementChecker<IrDeclarationReference>
internal typealias IrValueAccessChecker = IrElementChecker<IrValueAccessExpression>
internal typealias IrFunctionAccessChecker = IrElementChecker<IrFunctionAccessExpression>
internal typealias IrFunctionReferenceChecker = IrElementChecker<IrFunctionReference>
internal typealias IrMemberAccessChecker = IrElementChecker<IrMemberAccessExpression<IrFunctionSymbol>>
internal typealias IrStringConcatenationChecker = IrElementChecker<IrStringConcatenation>
internal typealias IrGetObjectValueChecker = IrElementChecker<IrGetObjectValue>
internal typealias IrGetValueChecker = IrElementChecker<IrGetValue>
internal typealias IrSetValueChecker = IrElementChecker<IrSetValue>
internal typealias IrGetFieldChecker = IrElementChecker<IrGetField>
internal typealias IrSetFieldChecker = IrElementChecker<IrSetField>
internal typealias IrCallChecker = IrElementChecker<IrCall>
internal typealias IrInstanceInitializerCallChecker = IrElementChecker<IrInstanceInitializerCall>
internal typealias IrTypeOperatorChecker = IrElementChecker<IrTypeOperatorCall>
internal typealias IrReturnChecker = IrElementChecker<IrReturn>
internal typealias IrPropertyReferenceChecker = IrElementChecker<IrPropertyReference>
internal typealias IrLocalDelegatedPropertyReferenceChecker = IrElementChecker<IrLocalDelegatedPropertyReference>
internal typealias IrConstChecker = IrElementChecker<IrConst>
internal typealias IrDelegatingConstructorCallChecker = IrElementChecker<IrDelegatingConstructorCall>
internal typealias IrLoopChecker = IrElementChecker<IrLoop>
internal typealias IrBreakContinueChecker = IrElementChecker<IrBreakContinue>
internal typealias IrThrowChecker = IrElementChecker<IrThrow>