/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.isLeftoverAfterObjectPurification
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.util.irError
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * Removes usages marked for removal in [PurifyObjectInstanceGettersLowering].
 *
 * Usages of `static_init` prevents declaration to be removed by DCE, so the lowering removes relevant call statements to reduce the
 * resulting bundle size.
 */
abstract class CleanupPurifiedLeftoverUsagesLowering(val context: JsCommonBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitBlockBody(body: IrBlockBody) {
                body.statements.removeLeftoverCalls()
                body.acceptChildrenVoid(this)
            }

            // Covers both IrBlock and IrComposite, so super static_init calls
            // emitted inside `irTry { irComposite { ... } }` are handled too.
            override fun visitContainerExpression(expression: IrContainerExpression) {
                expression.statements.removeLeftoverCalls()
                expression.acceptChildrenVoid(this)
            }

            // This check exist to prevent cases when leftover invocations are inserted inside complex expressions and can't be
            // easily wiped, meaning they weren't handled in above visit methods of standalone statements.
            override fun visitCall(expression: IrCall) {
                if (expression.symbol.owner.isLeftoverAfterObjectPurification) {
                    irError("Leftover call in a non-statement position") {
                        withIrEntry("expression", expression)
                        withIrEntry("container", container)
                    }
                }
                expression.acceptChildrenVoid(this)
            }
        })
    }

    private fun MutableList<IrStatement>.removeLeftoverCalls() {
        removeIf { it is IrCall && it.symbol.owner.isLeftoverAfterObjectPurification }
    }
}
