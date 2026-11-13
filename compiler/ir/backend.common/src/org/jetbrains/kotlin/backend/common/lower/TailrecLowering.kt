/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.collectTailRecursionCalls
import org.jetbrains.kotlin.backend.common.lower.optimizations.TailrecOptimization
import org.jetbrains.kotlin.backend.common.lower.optimizations.lowerTailRecursionCalls
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * This pass lowers tail recursion calls in `tailrec` functions.
 *
 * Note: it currently can't handle local functions and classes declared in default arguments.
 */
open class TailrecLowering(override val context: LoweringContext) : BodyLoweringPass, TailrecOptimization {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container is IrFunction) {
            // Lower local declarations
            irBody.acceptChildrenVoid(object : IrVisitorVoid() {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                    declaration.acceptChildrenVoid(this)
                    if (declaration.isTailrec) {
                        tryLowerRecursionCalls(declaration)
                    }
                }
            })

            if (container is IrSimpleFunction && container.isTailrec) {
                tryLowerRecursionCalls(container)
            }
        }
    }

    private fun tryLowerRecursionCalls(irFunction: IrFunction) {
        (val tailRecursionCalls = ir, val someCallsAreFromOtherFunctions = fromManyFunctions) = collectTailRecursionCalls(
            irFunction,
            ::followFunctionReference,
            ::followRichFunctionReference
        )

        if (tailRecursionCalls.isEmpty()) {
            return
        }

        lowerTailRecursionCalls(irFunction, someCallsAreFromOtherFunctions, tailRecursionCalls)
    }
}
