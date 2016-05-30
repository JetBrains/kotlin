/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.builtins.functions

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

class FunctionInvokeDescriptor private constructor(
        container: DeclarationDescriptor,
        original: FunctionInvokeDescriptor?,
        callableKind: CallableMemberDescriptor.Kind
) : SimpleFunctionDescriptorImpl(
        container,
        original,
        Annotations.EMPTY,
        Name.identifier("invoke"),
        callableKind,
        SourceElement.NO_SOURCE
) {
    // "p0", "p1", etc. should not be baked into the language
    override fun hasStableParameterNames(): Boolean = false

    override fun hasSynthesizedParameterNames(): Boolean = true

    override fun createSubstitutedCopy(
            newOwner: DeclarationDescriptor,
            original: FunctionDescriptor?,
            kind: CallableMemberDescriptor.Kind,
            newName: Name?,
            annotations: Annotations,
            source: SourceElement
    ): FunctionDescriptorImpl {
        return FunctionInvokeDescriptor(newOwner, original as FunctionInvokeDescriptor?, kind)
    }

    override fun isExternal(): Boolean = false

    override fun isInline(): Boolean = false

    override fun isTailrec(): Boolean = false

    companion object Factory {
        fun create(functionClass: FunctionClassDescriptor): FunctionInvokeDescriptor {
            val typeParameters = functionClass.declaredTypeParameters

            val result = FunctionInvokeDescriptor(functionClass, null, CallableMemberDescriptor.Kind.DECLARATION)
            result.initialize(
                    null,
                    functionClass.thisAsReceiverParameter,
                    listOf(),
                    typeParameters.takeWhile { it.variance == Variance.IN_VARIANCE }
                            .withIndex()
                            .map { createValueParameter(result, it.index, it.value) },
                    typeParameters.last().defaultType,
                    Modality.ABSTRACT,
                    Visibilities.PUBLIC
            )
            result.isOperator = true
            return result
        }

        private fun createValueParameter(
                containingDeclaration: FunctionInvokeDescriptor,
                index: Int,
                typeParameter: TypeParameterDescriptor
        ): ValueParameterDescriptor {
            val typeParameterName = typeParameter.name.asString()
            val name = when (typeParameterName) {
                "T" -> "instance"
                "E" -> "receiver"
                else -> {
                    // Type parameter "P1" -> value parameter "p1", "P2" -> "p2", etc.
                    typeParameterName.toLowerCase()
                }
            }

            return ValueParameterDescriptorImpl(
                    containingDeclaration, null, index,
                    Annotations.EMPTY,
                    Name.identifier(name),
                    typeParameter.defaultType,
                    /* declaresDefaultValue = */ false,
                    /* isCrossinline = */ false,
                    /* isNoinline = */ false,
                    /* isCoroutine = */ false,
                    /* varargElementType = */ null,
                    SourceElement.NO_SOURCE
            )
        }
    }
}
