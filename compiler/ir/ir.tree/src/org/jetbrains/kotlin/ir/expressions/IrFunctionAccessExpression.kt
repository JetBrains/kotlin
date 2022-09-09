/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.putValueArgumentViaSourceBasedArgumentIndex

// todo: autogenerate
/**
 * A non-leaf IR tree element.
 * @sample org.jetbrains.kotlin.ir.generator.IrTree.functionAccessExpression
 */
abstract class IrFunctionAccessExpression : IrMemberAccessExpression<IrFunctionSymbol>() {
    abstract var hasExtensionReceiver: Boolean
    abstract var contextReceiversCount: Int

    override var extensionReceiver: IrExpression?
        get() = if (hasExtensionReceiver) getValueArgument(contextReceiversCount) else null
        set(value) {
            if (value == null) return
            require(hasExtensionReceiver) { "Trying to set extension receiver for non-extension" }

            putValueArgument(contextReceiversCount, value)
        }

    override val receiversPrefixSize: Int
        get() = contextReceiversCount + (if (hasExtensionReceiver) 1 else 0)

    fun putExtensionReceiverAsArgument(expression: IrExpression) {
        require(hasExtensionReceiver) { "Trying to set extension receiver for non-extension function: $symbol" }

        putValueArgument(contextReceiversCount, expression)
    }

    fun putExtensionReceiverAsArgumentIfNotNull(expression: IrExpression?) {
        if (expression == null) return
        putExtensionReceiverAsArgument(expression)
    }
}
@ObsoleteDescriptorBasedAPI
inline fun <T : IrFunctionAccessExpression> T.mapValueParameters(transform: (ValueParameterDescriptor) -> IrExpression?): T =
    apply {
        val descriptor = symbol.descriptor as CallableDescriptor
        descriptor.valueParameters.forEach {
            putValueArgumentViaSourceBasedArgumentIndex(it.index, transform(it))
        }
    }
