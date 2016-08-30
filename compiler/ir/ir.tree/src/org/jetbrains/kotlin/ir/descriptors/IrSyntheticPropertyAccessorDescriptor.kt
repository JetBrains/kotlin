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

package org.jetbrains.kotlin.ir.descriptors

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertySetterDescriptorImpl


interface IrSyntheticPropertyAccessorDescriptor : PropertyAccessorDescriptor {
    enum class Kind {
        STATIC_PROPERTY
    }

    val kind: Kind
}

interface IrSyntheticPropertyGetterDescriptor : IrSyntheticPropertyAccessorDescriptor

interface IrSyntheticPropertySetterDescriptor : IrSyntheticPropertyAccessorDescriptor

class IrSyntheticPropertyGetterDescriptorImpl(
        correspondingProperty: PropertyDescriptor,
        override val kind: IrSyntheticPropertyAccessorDescriptor.Kind
) : PropertyGetterDescriptorImpl(
        correspondingProperty,
        Annotations.EMPTY,
        Modality.FINAL,
        correspondingProperty.visibility,
        true, // isDefault
        false, // isExternal
        false, // isInline
        CallableMemberDescriptor.Kind.SYNTHESIZED,
        null,
        correspondingProperty.source
), IrSyntheticPropertyGetterDescriptor {
    init {
        initialize(correspondingProperty.type)
    }
}

class IrSyntheticPropertySetterDescriptorImpl(
        correspondingProperty: PropertyDescriptor,
        override val kind: IrSyntheticPropertyAccessorDescriptor.Kind
) : PropertySetterDescriptorImpl(
        correspondingProperty,
        Annotations.EMPTY,
        Modality.FINAL,
        correspondingProperty.visibility,
        true, // isDefault
        false, // isExternal
        false, // isInline
        CallableMemberDescriptor.Kind.SYNTHESIZED,
        null,
        correspondingProperty.source
), IrSyntheticPropertySetterDescriptor {
    init {
        initializeDefault()
    }
}