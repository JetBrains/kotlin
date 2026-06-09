/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * Lightweight lowering for eliminating call to companion object getters with
 * unused result, when companion object is declared withing external interface.
 * Enables work with IDLs, like ones in kotlin-wrappers (KT-59082).
 */
class ExternalCompanionObjectsLowering(val context: WasmBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val externalCompanionObjectVars = irBody.collectExternalInterfaceCompanionObjectVars()
        irBody.eliminateTemporaryCompanionObjectInstanceVars(externalCompanionObjectVars)
    }
}

private fun IrBody.eliminateTemporaryCompanionObjectInstanceVars(excludedVars: Set<IrVariable>) {
    acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) =
            element.acceptChildrenVoid(this)

        override fun visitContainerExpression(expression: IrContainerExpression) {
            super.visitContainerExpression(expression)
            expression.statements -= excludedVars
        }

        override fun visitBlockBody(body: IrBlockBody) {
            super.visitBlockBody(body)
            body.statements -= excludedVars
        }
    })
}

private fun IrElement.collectExternalInterfaceCompanionObjectVars(): Set<IrVariable> {
    val collectedVars = mutableSetOf<IrVariable>()
    val parents = mutableMapOf<IrVariable, IrVariable?>()
    acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) =
            element.acceptChildrenVoid(this)

        // collecting all variables that are either initialized by external companion object or
        // by another external companion object variable
        override fun visitVariable(declaration: IrVariable) {
            val init = declaration.initializer
            if (init is IrGetObjectValue && isExternalInterfaceCompanion(init.symbol.owner)) {
                collectedVars += declaration
                parents[declaration] = null
            } else {
                val ownerIfInitializedByVariable = (declaration.initializer as? IrGetValue)?.symbol?.owner

                if (ownerIfInitializedByVariable in collectedVars) {
                    collectedVars += declaration
                    parents[declaration] = ownerIfInitializedByVariable as IrVariable
                } else {
                    declaration.acceptChildrenVoid(this)
                }
            }
        }

        override fun visitGetValue(expression: IrGetValue) {
            // variable is used - all parents also should be marked as used
            // children of used variables don't matter here -
            // unless they are themselves or their children used, they can be eliminated
            if (expression.symbol.owner in collectedVars) {
                var curSymbol: IrValueSymbol? = expression.symbol
                while (curSymbol?.owner in collectedVars) {
                    collectedVars.remove(curSymbol!!.owner)
                    curSymbol = parents[curSymbol.owner]?.symbol
                }
            }
        }
    })
    return collectedVars
}

private fun isExternalInterfaceCompanion(klass: IrClass): Boolean {
    if (!klass.isCompanion || !klass.isEffectivelyExternal()) return false
    val parent = klass.parent as? IrClass ?: return false
    return parent.isInterface
}
