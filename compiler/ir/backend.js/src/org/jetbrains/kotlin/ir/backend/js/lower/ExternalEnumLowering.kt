/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.WrappedPropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isEnumClass

// TODO: Shouldn't it be lazy pass?
class ExternalEnumLowering(val context: JsIrBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        if (!irClass.isExternal) return
        if (!irClass.isEnumClass) return

        irClass.declarations.filterIsInstance<IrEnumEntry>().forEach {
            val enumField = createFieldForEntry(it, irClass)
            context.enumEntryToGetInstanceFunction[it.symbol] = {
                JsIrBuilder.buildGetField(enumField.symbol, classAsReceiver(irClass), null, irClass.defaultType)
            }
        }
    }

    private fun classAsReceiver(irClass: IrClass): IrExpression {
        val intrinsic = context.intrinsics.jsClass
        return JsIrBuilder.buildCall(intrinsic, context.irBuiltIns.anyType, listOf(irClass.defaultType))
    }


    private fun createFieldForEntry(entry: IrEnumEntry, irClass: IrClass): IrField {
        val descriptor = WrappedPropertyDescriptor()
        val symbol = IrFieldSymbolImpl(descriptor)
        return entry.run {
            IrFieldImpl(startOffset, endOffset, origin, symbol, name, irClass.defaultType, Visibilities.DEFAULT_VISIBILITY, false, true, true ).also { f ->
                descriptor.bind(f)
                f.parent = irClass
                irClass.declarations += f

            }
        }
    }


}