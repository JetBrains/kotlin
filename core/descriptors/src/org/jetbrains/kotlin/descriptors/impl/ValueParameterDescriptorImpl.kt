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

package org.jetbrains.kotlin.descriptors.impl

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.utils.join

open class ValueParameterDescriptorImpl(
        containingDeclaration: CallableDescriptor,
        original: ValueParameterDescriptor?,
        override val index: Int,
        annotations: Annotations,
        name: Name,
        outType: KotlinType,
        private val declaresDefaultValue: Boolean,
        override val isCrossinline: Boolean,
        override val isNoinline: Boolean,
        override val varargElementType: KotlinType?,
        source: SourceElement
) : VariableDescriptorImpl(containingDeclaration, annotations, name, outType, source), ValueParameterDescriptor {

    companion object {
        @JvmStatic
        fun getDestructuringVariablesOrNull(valueParameterDescriptor: ValueParameterDescriptor) =
                (valueParameterDescriptor as? ValueParameterDescriptorImpl.WithDestructuringDeclaration)?.destructuringVariables

        @JvmStatic
        fun createWithDestructuringDeclarations(containingDeclaration: CallableDescriptor,
                                                original: ValueParameterDescriptor?,
                                                index: Int,
                                                annotations: Annotations,
                                                name: Name,
                                                outType: KotlinType,
                                                declaresDefaultValue: Boolean,
                                                isCrossinline: Boolean,
                                                isNoinline: Boolean, varargElementType: KotlinType?, source: SourceElement,
                                                destructuringVariables: (() -> List<VariableDescriptor>)?
        ): ValueParameterDescriptorImpl =
                if (destructuringVariables == null)
                    ValueParameterDescriptorImpl(containingDeclaration, original, index, annotations, name, outType,
                                                 declaresDefaultValue, isCrossinline, isNoinline, varargElementType, source)
                else
                    WithDestructuringDeclaration(containingDeclaration, original, index, annotations, name, outType,
                                                 declaresDefaultValue, isCrossinline, isNoinline, varargElementType, source,
                                                 destructuringVariables)
    }

    class WithDestructuringDeclaration internal constructor(
            containingDeclaration: CallableDescriptor,
            original: ValueParameterDescriptor?,
            index: Int,
            annotations: Annotations, name: Name,
            outType: KotlinType,
            declaresDefaultValue: Boolean,
            isCrossinline: Boolean,
            isNoinline: Boolean, varargElementType: KotlinType?,
            source: SourceElement,
            destructuringVariables: () -> List<VariableDescriptor>
    ) : ValueParameterDescriptorImpl(
            containingDeclaration, original, index, annotations, name, outType, declaresDefaultValue,
            isCrossinline, isNoinline,
            varargElementType, source) {
        // It's forced to be lazy because its resolution depends on receiver of relevant lambda, that is being created at the same moment
        // as value parameters.
        // Must be forced via ForceResolveUtil.forceResolveAllContents()
        val destructuringVariables by lazy(destructuringVariables)

        override fun copy(newOwner: CallableDescriptor, newName: Name, newIndex: Int): ValueParameterDescriptor {
            return WithDestructuringDeclaration(
                newOwner, null, newIndex, annotations, newName, type, declaresDefaultValue(),
                isCrossinline, isNoinline, varargElementType, SourceElement.NO_SOURCE
            ) { destructuringVariables }
        }
    }

    private val original: ValueParameterDescriptor = original ?: this

    override fun getContainingDeclaration() = super.getContainingDeclaration() as CallableDescriptor

    override fun declaresDefaultValue(): Boolean {
        return declaresDefaultValue && (containingDeclaration as CallableMemberDescriptor).kind.isReal
    }

    override fun getOriginal() = if (original === this) this else original.original

    override fun substitute(substitutor: TypeSubstitutor): ValueParameterDescriptor {
        if (substitutor.isEmpty) return this
        throw UnsupportedOperationException() // TODO
    }

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
        return visitor.visitValueParameterDescriptor(this, data)
    }

    override fun isVar() = false

    override fun getCompileTimeInitializer() = null
    override fun copy(newOwner: CallableDescriptor, newName: Name, newIndex: Int): ValueParameterDescriptor {
        return ValueParameterDescriptorImpl(
                newOwner, null, newIndex, annotations, newName, type, declaresDefaultValue(),
                isCrossinline, isNoinline, varargElementType, SourceElement.NO_SOURCE
        )
    }

    override fun getVisibility() = Visibilities.LOCAL

    override fun getOverriddenDescriptors(): Collection<ValueParameterDescriptor> {
        return containingDeclaration.overriddenDescriptors.map {
            it.valueParameters[index]
        }
    }
}
