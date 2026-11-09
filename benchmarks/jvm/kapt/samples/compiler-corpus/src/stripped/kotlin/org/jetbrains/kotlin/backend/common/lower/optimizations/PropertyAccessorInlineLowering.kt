/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.optimizations

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.backend.common.ir.isPure
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.isTopLevel
import org.jetbrains.kotlin.ir.util.isTrivialGetter
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

open class PropertyAccessorInlineLowering(
    private val context: CommonBackendContext,
) : BodyLoweringPass {

    fun IrProperty.isSafeToInlineInClosedWorld() =
        isTopLevel || (modality === Modality.FINAL || visibility == DescriptorVisibilities.PRIVATE) || (parent as IrClass).modality === Modality.FINAL

    open fun IrProperty.isSafeToInline(accessContainer: IrDeclaration): Boolean = TODO()

    // TODO: implement general function inlining optimization and replace it with
    private inner class AccessorInliner(val container: IrDeclaration) : IrElementTransformerVoid() {

        private val unitType = context.irBuiltIns.unitType

        private fun canBeInlined(callee: IrSimpleFunction): Boolean { TODO() }

        override fun visitCall(expression: IrCall): IrExpression { TODO() }

        private fun tryInlineSimpleGetter(call: IrCall, callee: IrSimpleFunction, backingField: IrField): IrExpression? { TODO() }

        private fun tryInlineSimpleSetter(call: IrCall, callee: IrSimpleFunction, backingField: IrField): IrExpression? { TODO() }

        private fun isSimpleSetter(callee: IrSimpleFunction, backingField: IrField): Boolean { TODO() }
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) { TODO() }
}
