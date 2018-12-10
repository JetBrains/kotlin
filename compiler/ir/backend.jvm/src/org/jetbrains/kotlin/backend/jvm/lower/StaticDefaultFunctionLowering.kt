/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

internal val staticDefaultFunctionPhase = makeIrFilePhase(
    ::StaticDefaultFunctionLowering,
    name = "StaticDefaultFunction",
    description = "Generate static functions for default parameters"
)

private class StaticDefaultFunctionLowering() : IrElementTransformerVoid(), ClassLoweringPass {
    constructor(@Suppress("UNUSED_PARAMETER") context: BackendContext) : this()

    override fun lower(irClass: IrClass) {
        irClass.accept(this, null)
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        return if (declaration.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER && declaration.dispatchReceiverParameter != null) {
            createStaticFunctionWithReceivers(declaration.parent, declaration.name, declaration)
        } else {
            super.visitFunction(declaration)
        }
    }
}