/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.inline.KlibSyntheticAccessorGenerator
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Generates synthetic accessor functions for private declarations that are referenced from non-private inline functions,
 * so that after those functions are inlined, there'll be no visibility violations.
 *
 * There are a few important assumptions that this lowering relies on:
 * - It's executed in a KLIB-based backend. It's not designed to work with the JVM backend because the visibility rules on JVM are
 *   stricter.
 * - By the point it's executed, all _private_ inline functions have already been inlined.
 */
class SyntheticAccessorLowering(context: CommonBackendContext) : FileLoweringPass {
    private val accessorGenerator = KlibSyntheticAccessorGenerator(context)

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(Transformer())
    }

    private inner class Transformer : IrElementTransformerVoid() {
        private var currentInlineFunction: IrFunction? = null
        private var insideBody = false

        override fun visitFunction(declaration: IrFunction): IrStatement {
            val previousInlineFunction = currentInlineFunction
            try {
                currentInlineFunction = if (declaration.isInline) {
                    declaration.takeIf {
                        // By the time this lowering is executed, there must be no private inline functions, however, there are exceptions, for example,
                        // suspendCoroutineUninterceptedOrReturn, which are somewhat magical.
                        // If we encounter one, just ignore it.
                        !declaration.isConsideredAsPrivateForInlining()
                    }
                } else {
                    previousInlineFunction
                }

                return declaration.factory.stageController.restrictTo(declaration) {
                    super.visitFunction(declaration)
                }
            } finally {
                currentInlineFunction = previousInlineFunction
            }
        }

        override fun visitBody(body: IrBody): IrBody {
            val previousInsideBody = insideBody
            try {
                insideBody = true
                return super.visitBody(body)
            } finally {
                insideBody = previousInsideBody
            }
        }

        override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
            if (currentInlineFunction == null || !insideBody || !expression.symbol.owner.isAbiPrivate)
                return super.visitFunctionAccess(expression)

            // TODO(KT-69527): Set the proper visibility for the accessor (the max visibility of all the inline functions that reference it)
            val accessor = accessorGenerator.getSyntheticFunctionAccessor(expression, null)
            val accessorExpression = accessorGenerator.modifyFunctionAccessExpression(expression, accessor.symbol)

            return super.visitFunctionAccess(accessorExpression)
        }
    }

    companion object {
        // TODO: Take into account visibilities of containers
        // TODO(KT-69565): It's not enough to just look at the visibility, since the declaration may be private inside a local class
        //   and accessed only within that class. For such cases we shouldn't generate an accessor.
        private val IrDeclarationWithVisibility.isAbiPrivate: Boolean
            get() = DescriptorVisibilities.isPrivate(visibility) || visibility == DescriptorVisibilities.LOCAL
    }
}
