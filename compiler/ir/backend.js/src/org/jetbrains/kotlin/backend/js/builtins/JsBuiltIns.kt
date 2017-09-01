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

package org.jetbrains.kotlin.backend.js.builtins

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltinValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltinsPackageFragmentDescriptorImpl
import org.jetbrains.kotlin.ir.descriptors.IrSimpleBuiltinOperatorDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

class JsBuiltIns(module: IrModuleFragment) {
    private val jsIntrinsicsPackage = IrBuiltinsPackageFragmentDescriptorImpl(module.descriptor.builtIns.builtInsModule, PACKAGE_FQ_NAME)

    val isUndefined = defineOperator(IS_UNDEFINED_NAME, module.irBuiltins.bool, listOf(module.irBuiltins.anyN))

    // TODO: avoid duplication with IrBuiltIns
    private fun defineOperator(name: String, returnType: KotlinType, valueParameterTypes: List<KotlinType>): FunctionDescriptor {
        val operatorDescriptor = IrSimpleBuiltinOperatorDescriptorImpl(jsIntrinsicsPackage, Name.identifier(name), returnType)
        for ((i, valueParameterType) in valueParameterTypes.withIndex()) {
            operatorDescriptor.addValueParameter(
                    IrBuiltinValueParameterDescriptorImpl(operatorDescriptor, Name.identifier("arg$i"), i, valueParameterType)
            )
        }
        return operatorDescriptor
    }

    companion object {
        val PACKAGE_FQ_NAME = FqName("kotlin.internal.ir.js")

        private const val IS_UNDEFINED_NAME = "IS_UNDEFINED"
        val IS_UNDEFINED_FQ_NAME = PACKAGE_FQ_NAME.child(Name.identifier(IS_UNDEFINED_NAME))
    }
}
