/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.inline.SyntheticAccessorGenerator
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.util.parentDeclarationsWithSelf
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
class SyntheticAccessorLowering(
    context: CommonBackendContext,
) : BodyLoweringPass {
    private val accessorGenerator = SyntheticAccessorGenerator(context)
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val inlineFunction = container.parentDeclarationsWithSelf.firstOrNull { it is IrFunction && it.isInline } as? IrFunction
            ?: return
        if (inlineFunction.isConsideredAsPrivateForInlining()) {
            // By the time this lowering is executed, there must be no private inline functions, however, there are exceptions, for example,
            // suspendCoroutineUninterceptedOrReturn, which are somewhat magical.
            // If we encounter one, just ignore it.
            return
        }
        irBody.transformChildrenVoid(SyntheticAccessorTransformer(accessorGenerator))
    }
}

private class SyntheticAccessorTransformer(
    private val accessorGenerator: SyntheticAccessorGenerator<CommonBackendContext>,
) : IrElementTransformerVoid() {

    // TODO: Take into account visibilities of containers
    // TODO(KT-69565): It's not enough to just look at the visibility, since the declaration may be private inside a local class
    //   and accessed only within that class. For such cases we shouldn't generate an accessor.
    private val IrDeclarationWithVisibility.isAbiPrivate: Boolean
        get() = DescriptorVisibilities.isPrivate(visibility) || visibility == DescriptorVisibilities.LOCAL

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        if (!expression.symbol.owner.isAbiPrivate) {
            return super.visitFunctionAccess(expression)
        }
        val accessor = accessorGenerator.getSyntheticFunctionAccessor(expression, emptyList())

        // FIXME: This can easily lead to the same accessor being added multiple times to the same declaration container.
        (accessor.parent as IrDeclarationContainer).declarations.add(accessor)

        // TODO(KT-69527): Set the proper visibility for the accessor (the max visibility of all the inline functions that reference it)
        return super.visitFunctionAccess(accessorGenerator.modifyFunctionAccessExpression(expression, accessor.symbol))
    }
}
