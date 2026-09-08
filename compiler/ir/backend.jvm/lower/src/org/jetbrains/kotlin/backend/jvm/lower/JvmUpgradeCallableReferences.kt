/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.UpgradeCallableReferences
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isSuspendFunction

internal class JvmUpgradeCallableReferences(context: JvmBackendContext) : UpgradeCallableReferences(
    context = context,
    upgradeSamConversions = true,
) {
    // FIR2IR casts function references to approximated function types for projected SAM types (KT-51868).
    // Unwrap only those casts; LambdaMetafactoryArgumentsBuilder handles the resulting mismatch (KT-57995).
    override fun getSamConversionArgument(argument: IrExpression): IrExpression =
        if (argument is IrTypeOperatorCall &&
            argument.operator == IrTypeOperator.IMPLICIT_CAST &&
            argument.argument is IrRichFunctionReference &&
            (argument.typeOperand.isFunction() || argument.typeOperand.isSuspendFunction())
        ) {
            argument.argument
        } else {
            argument
        }
}
