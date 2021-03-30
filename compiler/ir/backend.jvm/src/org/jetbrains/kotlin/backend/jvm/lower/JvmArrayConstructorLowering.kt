/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.ArrayConstructorTransformer
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class JvmArrayConstructorLowering(val context: JvmBackendContext) : IrElementTransformerVoidWithContext(), BodyLoweringPass {

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(JvmArrayConstructorTransformer(context, container as IrSymbolOwner))
    }
}

class JvmArrayConstructorTransformer(
    private val jvmContext: JvmBackendContext,
    container: IrSymbolOwner
) : ArrayConstructorTransformer(jvmContext, container) {

    override fun beforeArrayConstructorBody(builder: IrBlockBuilder) {
        with(builder) {
            +irCall(jvmContext.ir.symbols.beforeInlineCall)
        }
    }

    override fun afterArrayConstructorBody(builder: IrBlockBuilder) {
        with(builder) {
            +irCall(jvmContext.ir.symbols.afterInlineCall)
        }
    }
}