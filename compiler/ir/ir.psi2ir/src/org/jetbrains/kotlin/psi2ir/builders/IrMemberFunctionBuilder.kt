/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.psi2ir.builders

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunctionImpl
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.Scope

class IrMemberFunctionBuilder(
        context: GeneratorContext,
        val irClass: IrClassImpl,
        val function: FunctionDescriptor,
        val origin: IrDeclarationOrigin,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET
) : IrBlockBodyBuilder(context, Scope(function), startOffset, endOffset) {
    inline fun addToClass(body: IrMemberFunctionBuilder.() -> Unit) {
        val irFunction = IrFunctionImpl(startOffset, endOffset, origin, function)
        body()
        irFunction.body = doBuild()
        irClass.addMember(irFunction)
    }
}