/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.isNumber
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.types.expressions.OperatorConventions

internal val replaceNumberToCharCallSitesPhase = makeIrFilePhase(
    ::ReplaceNumberToCharCallSitesLowering,
    name = "ReplaceNumberToCharCallSites",
    description = "Replace `Number.toChar` call sites with `toInt().toChar()`",
)

// This lowering replaces call sites of the form `x.toChar()` to `x.toInt().toChar()`, but only if the declaration of `toChar`
// in the scope of `x` comes from the library class `kotlin.Number`. For example, if `x` is some `MyNumber` which overrides
// `toChar`, we won't replace it because there might be some custom logic, different from `toInt().toChar()`.
//
// This allows us to migrate usages of deprecated `Number.toChar` less painfully in order to remove it in the future (KT-56822).
// Also, this allows to invoke `toChar` on `Number` subclasses declared in Java, which do not have it declared, even though the
// compiler sees it there because `java.lang.Number` is mapped to `kotlin.Number`.
class ReplaceNumberToCharCallSitesLowering(val context: JvmBackendContext) : FileLoweringPass, IrElementVisitorVoid {
    override fun lower(irFile: IrFile) {
        irFile.acceptChildren(this, null)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitCall(expression: IrCall) {
        expression.acceptChildren(this, null)

        val callee = expression.symbol.owner
        if (callee.name != OperatorConventions.CHAR) return

        val declaration = callee.resolveFakeOverride() ?: callee
        val declaringClassType = declaration.dispatchReceiverParameter?.type ?: return
        if (!declaringClassType.isNumber()) return

        val dispatchReceiver = expression.dispatchReceiver ?: return
        expression.dispatchReceiver = IrCallImpl(
            dispatchReceiver.startOffset, dispatchReceiver.endOffset,
            context.irBuiltIns.intType, context.irBuiltIns.numberClass.functionByName("toInt"), 0, 0,
        ).also { toInt ->
            toInt.dispatchReceiver = dispatchReceiver
        }

        expression.symbol = context.irBuiltIns.intClass.functionByName("toChar")
    }
}
