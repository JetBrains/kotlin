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

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.lower.DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.resolve.DescriptorUtils

class StaticDefaultFunctionLowering(val state: GenerationState) : IrElementTransformerVoid(), ClassLoweringPass {

    override fun lower(irClass: IrClass) {
        val descriptor = irClass.descriptor
        if (DescriptorUtils.isInterface(descriptor) || DescriptorUtils.isAnnotationClass(descriptor)) {
            return
        }
        irClass.accept(this, null)
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        if (declaration.origin == DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER && declaration.dispatchReceiverParameter != null) {
            val newFunction = createStaticFunctionWithReceivers(declaration.descriptor.containingDeclaration as ClassDescriptor, declaration.descriptor.name, declaration.descriptor, declaration.descriptor.dispatchReceiverParameter!!.type)
            return newFunction.createFunctionAndMapVariables(declaration)
        }
        else {
            return super.visitFunction(declaration)
        }
    }
}