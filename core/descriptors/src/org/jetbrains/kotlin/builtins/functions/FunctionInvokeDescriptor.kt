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

import org.jetbrains.kotlin.builtins.extractParameterNameFromFunctionTypeArgument
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions

class FunctionInvokeDescriptor private constructor(
        container: DeclarationDescriptor,
        original: FunctionInvokeDescriptor?,
        callableKind: CallableMemberDescriptor.Kind,
        isSuspend: Boolean
) : SimpleFunctionDescriptorImpl(
        container,
        original,
        Annotations.EMPTY,
        OperatorNameConventions.INVOKE,
        callableKind,
        SourceElement.NO_SOURCE
) {
    init {
        this.isOperator = true
        this.isSuspend = isSuspend
        this.setHasStableParameterNames(false)
    }

    /**
     * Returns true if this `invoke` has so much parameters that it makes sense to wrap them into array and make this function have a single
     * `vararg Any?` parameter in the binaries, on platforms where function types are represented as separate classes (JVM, Native).
     *
     * For example, on JVM there are 23 function classes with the fixed number of parameters (`Function0` ... `Function22`), and another
     * class representing the function with big arity, with a variable number of parameters (`FunctionN`). Whenever `invoke` of a function
     * type with big arity is called, the JVM back-end wraps all arguments into an array and passes it to `FunctionN.invoke`.
     */
    @get:JvmName("hasBigArity")
    val hasBigArity: Boolean
        get() = valueParameters.size >= BuiltInFunctionArity.BIG_ARITY

    override fun doSubstitute(configuration: CopyConfiguration): FunctionDescriptor? {
        val substituted = super.doSubstitute(configuration) as FunctionInvokeDescriptor? ?: return null
        if (substituted.valueParameters.none { it.type.extractParameterNameFromFunctionTypeArgument() != null }) return substituted
        val parameterNames = substituted.valueParameters.map { it.type.extractParameterNameFromFunctionTypeArgument() }
        return substituted.replaceParameterNames(parameterNames)
    }

    override fun createSubstitutedCopy(
            newOwner: DeclarationDescriptor,
            original: FunctionDescriptor?,
            kind: CallableMemberDescriptor.Kind,
            newName: Name?,
            annotations: Annotations,
            source: SourceElement
    ): FunctionDescriptorImpl {
        return FunctionInvokeDescriptor(newOwner, original as FunctionInvokeDescriptor?, kind, isSuspend)
    }

    override fun isExternal(): Boolean = false

    override fun isInline(): Boolean = false

    override fun isTailrec(): Boolean = false

    private fun replaceParameterNames(parameterNames: List<Name?>): FunctionDescriptor {
        val indexShift = valueParameters.size - parameterNames.size
        assert(indexShift == 0 || indexShift == 1) // indexShift == 1 for extension function type

        val newValueParameters = valueParameters.map {
            var newName = it.name
            val parameterIndex = it.index
            val nameIndex = parameterIndex - indexShift
            if (nameIndex >= 0) {
                val parameterName = parameterNames[nameIndex]
                if (parameterName != null) {
                    newName = parameterName
                }
            }
            it.copy(this, newName, parameterIndex)
        }

        val copyConfiguration = newCopyBuilder(TypeSubstitutor.EMPTY)
                .setHasSynthesizedParameterNames(parameterNames.any { it == null })
                .setValueParameters(newValueParameters)
                .setOriginal(original)

        return super.doSubstitute(copyConfiguration)!!
    }

    companion object Factory {
        fun create(functionClass: FunctionClassDescriptor, isSuspend: Boolean): FunctionInvokeDescriptor {
            val typeParameters = functionClass.declaredTypeParameters

            val result = FunctionInvokeDescriptor(functionClass, null, CallableMemberDescriptor.Kind.DECLARATION, isSuspend)
            result.initialize(
                    null,
                    functionClass.thisAsReceiverParameter,
                    listOf(), listOf(),
                    typeParameters.takeWhile { it.variance == Variance.IN_VARIANCE }
                            .withIndex()
                            .map { createValueParameter(result, it.index, it.value) },
                    typeParameters.last().defaultType,
                    Modality.ABSTRACT,
                    DescriptorVisibilities.PUBLIC
            )
            result.setHasSynthesizedParameterNames(true)
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
                    typeParameterName.lowercase()
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
                    /* varargElementType = */ null,
                    SourceElement.NO_SOURCE
            )
        }
    }
}
