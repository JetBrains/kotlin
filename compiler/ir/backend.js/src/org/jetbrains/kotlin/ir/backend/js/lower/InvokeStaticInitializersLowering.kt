/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsStatementOrigins
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.util.*

class InvokeStaticInitializersLowering(val context: JsIrBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container !is IrConstructor) return
        if (container.parentClassOrNull?.isEnumClass == true) return

        val irClass = container.constructedClass
        if (irClass.isEffectivelyExternal()) {
            return
        }

        val companionObject = irClass.companionObject() ?: return

        val instance = context.mapping.objectToGetInstanceFunction[companionObject] ?: return

        val getInstanceCall = IrCallImpl(
            irClass.startOffset,
            irClass.endOffset,
            context.irBuiltIns.unitType,
            instance.symbol,
            0,
            0,
            JsStatementOrigins.SYNTHESIZED_STATEMENT
        )

        (irBody as IrStatementContainer).statements.add(0, getInstanceCall)
    }
}
