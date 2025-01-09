/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.isJvmInterface
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid

/**
 * Resolves calls to Object methods on interface types to virtual methods.
 */
@PhaseDescription(name = "InterfaceObjectCalls")
internal class InterfaceObjectCallsLowering(val context: JvmBackendContext) : IrVisitorVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) = irFile.acceptChildren(this, null)

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitCall(expression: IrCall) {
        expression.acceptChildren(this, null)

        if (expression.superQualifierSymbol != null && !expression.isSuperToAny()) return

        val callee = expression.symbol.owner
        if (!callee.isMethodOfAny()) return
        if (!callee.hasInterfaceParent() && expression.dispatchReceiver?.run { type.erasedUpperBound.isJvmInterface } != true) return

        val resolved = callee.resolveFakeOverride() ?: return
        expression.symbol = resolved.symbol
        if (expression.superQualifierSymbol != null) {
            expression.superQualifierSymbol = context.irBuiltIns.anyClass
        }
    }
}
