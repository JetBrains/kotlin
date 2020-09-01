/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.lower.SingleAbstractMethodLowering
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.render

internal class NativeSingleAbstractMethodLowering(context: Context) : SingleAbstractMethodLowering(context) {
    override fun getWrapperVisibility(expression: IrTypeOperatorCall, scopes: List<ScopeWithIr>) =
            DescriptorVisibilities.PRIVATE

    override fun getSuperTypeForWrapper(typeOperand: IrType): IrType {
        return typeOperand.classOrNull?.defaultType ?: error("Unsupported SAM conversion: ${typeOperand.render()}")
    }

    override val IrType.needEqualsHashCodeMethods: Boolean
        get() = false
}
