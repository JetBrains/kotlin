/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.error

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.name.Name

class ErrorPropertyDescriptor : PropertyDescriptor by (
        PropertyDescriptorImpl.create(
            ErrorUtils.errorClass, Annotations.EMPTY, Modality.OPEN,
            DescriptorVisibilities.PUBLIC, true, Name.special(ErrorEntity.ERROR_PROPERTY.debugText),
            CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE,
            false, false, false, false, false, false
        ).apply {
            setType(ErrorUtils.errorPropertyType, emptyList(), null, null, emptyList())
        }
    )
