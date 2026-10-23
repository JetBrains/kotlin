/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Throws a proper exception for calls returning value of type [Nothing].
 */
open class KotlinNothingValueExceptionLowering(
    val backendContext: CommonBackendContext, val skip: (IrDeclaration) -> Boolean = { false }
) : BodyLoweringPass {
    override fun lower(irFile: IrFile) =
        runOnFilePostfix(irFile, withLocalDeclarations = true)

    override fun lower(irBody: IrBody, container: IrDeclaration) { TODO() }

    private inner class Transformer(val parent: IrSymbol) : IrElementTransformerVoid() {
        override fun visitBody(body: IrBody): IrBody = TODO()

        override fun visitCall(expression: IrCall): IrExpression = TODO()
    }
}
