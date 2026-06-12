/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.ir.isTmpForInline
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.Companion.IR_TEMPORARY_VARIABLE
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.visitors.IrVisitor

/**
 * Lightweight lowering for eliminating call to companion object getters with
 * unused result, when companion object is declared withing external interface.
 * Enables work with IDLs, like ones in kotlin-wrappers (KT-59082):
 *
 * external interface Flags {
 * }
 *
 * inline val Flags.Companion.ON get() = "ON".asDynamic().unsafeCast<Flags>()
 * inline val Flags.Companion.OFF get() = "OFF".asDynamic().unsafeCast<Flags>()
 *
 */
class ExternalCompanionObjectsLowering(val context: WasmBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val varsByContainer = irBody.collectExternalInterfaceCompanionObjectVars()
        for ((statementContainer, vars) in varsByContainer) {
            statementContainer.statements.removeAll(vars.toHashSet())
        }
    }
}

/**
 *  We should track reassignments, because accesses to the companion object variables
 *  can be lowered with the sequences of reassignments, like in the following scheme:
 *
 *     call $Companion_$external_object_getInstance__externalAdapter___fun_3796
 *     local.set $1_~tmp0
 *     block (result externref)
 *         ;; Inlined call of `EVENODD`
 *         nop
 *         local.get $1_~tmp0  ;; type: <root>.CanvasFillRule.Companion
 *         local.set $2_~this
 *
 *         ;; const string: "evenodd"
 *         i32.const 735
 *         global.get $string_7___g_735
 *         call $_stringLiteralJsString___fun_3801
 *
 *         call $kotlin.js.toJsString___fun_3094
 *         br 0
 *     end
 *
 *  Expressions of other types, where external companion object
 *  variables are used, are treated as unpure and elimination
 *  is not happening for the variables mentioned in those expressions.
 */
private fun IrBody.collectExternalInterfaceCompanionObjectVars(): Map<IrStatementContainer, List<IrVariable>> {

    // Stores sequences of reassignments marking [initializerOwner] of initialized variables.
    val initializerOwner = HashMap<IrVariable, IrVariable?>()
    // Remembers the statement container for each collected variable to speed up the erasure stage.
    val containerByVar = HashMap<IrVariable, IrStatementContainer>()

    // current container is tracked with the data variable of IrVisitor
    accept(object : IrVisitor<Unit, IrStatementContainer?>() {
        override fun visitElement(element: IrElement, data: IrStatementContainer?) {
            element.acceptChildren(this, data)
        }

        override fun visitBlockBody(body: IrBlockBody, data: IrStatementContainer?) {
            body.acceptChildren(this, body)
        }

        override fun visitContainerExpression(expression: IrContainerExpression, data: IrStatementContainer?) {
            expression.acceptChildren(this, expression)
        }

        // Collects all temporary variables that are either
        // initialized by external companion object or
        // by another external companion object variable
        override fun visitVariable(declaration: IrVariable, data: IrStatementContainer?) {

            if (!declaration.isTmpForInline && declaration.origin != IR_TEMPORARY_VARIABLE) {
                super.visitVariable(declaration, data)
                return
            }

            val init = declaration.initializer
            if (init is IrGetObjectValue && isExternalInterfaceCompanion(init.symbol.owner)) {
                initializerOwner[declaration] = null
                containerByVar[declaration] = data!!
            } else {
                val ownerIfInitializedByVariable = (declaration.initializer as? IrGetValue)?.symbol?.owner

                if (ownerIfInitializedByVariable in initializerOwner) {
                    initializerOwner[declaration] = ownerIfInitializedByVariable as IrVariable
                    containerByVar[declaration] = data!!
                } else {
                    declaration.acceptChildren(this, data)
                }
            }
        }

        override fun visitGetValue(expression: IrGetValue, data: IrStatementContainer?) {
            if (expression.symbol.owner in initializerOwner) {
                var curSymbol: IrValueSymbol? = expression.symbol
                while (curSymbol?.owner in initializerOwner) {
                    initializerOwner.remove(curSymbol!!.owner)
                    curSymbol = initializerOwner[curSymbol.owner]?.symbol
                }
            }
        }
    }, null)
    return initializerOwner.keys.groupBy { containerByVar.getValue(it) }
}

private fun isExternalInterfaceCompanion(klass: IrClass): Boolean {
    if (!klass.isCompanion || !klass.isEffectivelyExternal()) return false
    val parent = klass.parent as? IrClass ?: return false
    return parent.isInterface
}
