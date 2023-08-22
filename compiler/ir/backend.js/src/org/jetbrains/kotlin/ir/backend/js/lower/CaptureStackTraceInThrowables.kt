/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrRawFunctionReferenceImpl
import org.jetbrains.kotlin.ir.util.isSubclassOf
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.Name

val ES6_THROWABLE_CONSTRUCTOR_SLOT by IrDeclarationOriginImpl
/**
 * Capture stack trace in primary constructors of Throwable
 */
class CaptureStackTraceInThrowables(val context: JsIrBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container !is IrConstructor || !container.isPrimary)
            return

        val klass = container.parentAsClass

        if (!klass.isSubclassOf(context.irBuiltIns.throwableClass.owner))
            return

        val statements = (irBody as? IrBlockBody)?.statements ?: return
        val delegatingConstructorCallIndex = statements.indexOfLast { it is IrDelegatingConstructorCall }

        statements.add(delegatingConstructorCallIndex + 1, JsIrBuilder.buildCall(context.intrinsics.captureStack).also { call ->
            val self = klass.thisReceiver!!.symbol

            val constructorRef = if (context.es6mode) {
                JsIrBuilder.buildGetField(klass.addThrowableConstructorSlot().symbol, JsIrBuilder.buildGetValue(self))
            } else {
                JsIrBuilder.buildRawReference(container.symbol, context.irBuiltIns.anyType)
            }

            call.putValueArgument(0, JsIrBuilder.buildGetValue(self))
            call.putValueArgument(1, constructorRef)
        })
    }

    private fun IrClass.addThrowableConstructorSlot(): IrField {
        return factory.buildField {
            type = context.dynamicType
            isFinal = false
            isExternal = false
            isStatic = false
            metadata = null
            name = Name.identifier(Namer.THROWABLE_CONSTRUCTOR)
            visibility = DescriptorVisibilities.PRIVATE
            origin = ES6_THROWABLE_CONSTRUCTOR_SLOT
        }.apply {
            parent = this@addThrowableConstructorSlot
            declarations.add(this)
        }
    }
}
