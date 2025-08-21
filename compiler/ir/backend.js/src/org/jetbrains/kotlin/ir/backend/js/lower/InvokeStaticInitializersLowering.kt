/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsStatementOrigins
import org.jetbrains.kotlin.ir.backend.js.objectGetInstanceFunction
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.util.*

/**
 * Invokes companion object's initializers from companion object in object constructor.
 */
class InvokeStaticInitializersLowering(val context: JsCommonBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container !is IrConstructor) return
        if (container.parentClassOrNull?.isEnumClass == true) return

        val irClass = container.constructedClass
        if (irClass.isEffectivelyExternal()) {
            return
        }

        val companionObject = irClass.companionObject() ?: return

        val instance = companionObject.objectGetInstanceFunction ?: return

        val getInstanceCall = IrCallImpl(
            irClass.startOffset,
            irClass.endOffset,
            context.irBuiltIns.unitType,
            instance.symbol,
            typeArgumentsCount = 0,
            origin = JsStatementOrigins.SYNTHESIZED_STATEMENT
        )

        (irBody as IrStatementContainer).statements.add(0, getInstanceCall)
    }
}
