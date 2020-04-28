/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.DefaultParameterInjector
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.defaultValue
import org.jetbrains.kotlin.backend.jvm.ir.getJvmVisibilityOfDefaultArgumentStub
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType

class JvmDefaultParameterInjector(context: JvmBackendContext) :
    DefaultParameterInjector(context, skipInline = false, skipExternalMethods = false, createStaticDefaultStubs = true) {

    override val context: JvmBackendContext get() = super.context as JvmBackendContext

    override fun nullConst(startOffset: Int, endOffset: Int, irParameter: IrValueParameter): IrExpression? =
        nullConst(startOffset, endOffset, irParameter.type)

    override fun nullConst(startOffset: Int, endOffset: Int, type: IrType): IrExpression {
        return type.defaultValue(startOffset, endOffset, context)
    }

    override fun defaultArgumentStubVisibility(function: IrFunction) = function.getJvmVisibilityOfDefaultArgumentStub()
}
