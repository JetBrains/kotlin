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
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.KotlinType

sealed class LocalVariableAccessorDescriptor(
    final override val correspondingVariable: LocalVariableDescriptor,
    isGetter: Boolean
) : SimpleFunctionDescriptorImpl(
    correspondingVariable.containingDeclaration,
    null,
    Annotations.EMPTY,
    Name.special((if (isGetter) "<get-" else "<set-") + correspondingVariable.name + ">"),
    CallableMemberDescriptor.Kind.SYNTHESIZED,
    SourceElement.NO_SOURCE
), VariableAccessorDescriptor {
    class Getter(correspondingVariable: LocalVariableDescriptor) : LocalVariableAccessorDescriptor(correspondingVariable, true)
    class Setter(correspondingVariable: LocalVariableDescriptor) : LocalVariableAccessorDescriptor(correspondingVariable, false)

    init {
        val valueParameters =
            if (isGetter) emptyList() else listOf(createValueParameter(Name.identifier("value"), correspondingVariable.type))
        val returnType =
            if (isGetter) correspondingVariable.type else correspondingVariable.builtIns.unitType
        @Suppress("LeakingThis")
        initialize(null, null, emptyList(), emptyList(), valueParameters, returnType, Modality.FINAL, DescriptorVisibilities.LOCAL)
    }

    private fun createValueParameter(name: Name, type: KotlinType): ValueParameterDescriptorImpl {
        return ValueParameterDescriptorImpl(
            this, null, 0, Annotations.EMPTY, name, type,
            false, false, false, null, SourceElement.NO_SOURCE
        )
    }

}
