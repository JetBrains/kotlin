/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.irFlag
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class MarkPubliclyAccessibleInlineFunctionsLowering(private val context: LoweringContext) : IrVisitorVoid(), ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        irModule.acceptVoid(this)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        //if (declaration.isInline && !declaration.isEffectivelyPrivate()) {
        if (declaration.isInline && !declaration.symbol.isConsideredAsPrivateForInlining()) {
            // By the time this lowering is executed, there must be no private inline functions; however,
            // there are exceptions, for example, `suspendCoroutineUninterceptedOrReturn` which are somewhat magical.
            // If we encounter one, ignore it.
            declaration.isPubliclyAccessibleInline = true
            declaration.acceptVoid(MarkAsAccessibleVisitor)
        }

        super.visitSimpleFunction(declaration)
    }

    private object MarkAsAccessibleVisitor : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitCall(expression: IrCall, data: Nothing?) {
            val callee = expression.symbol.owner
            if (callee.isInline) {
                callee.isPubliclyAccessibleInline = true
                callee.body?.acceptVoid(this)
                callee.parameters.forEach { param ->
                    if (expression.arguments[param] == null) {
                        param.defaultValue?.acceptVoid(this)
                    }
                }
            }

            super.visitCall(expression, data)
        }
    }
}

internal var IrFunction.isPubliclyAccessibleInline by irFlag(copyByDefault = true)