/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.lazy.descriptors.script

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertySetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyScriptDescriptor

class ScriptEnvironmentPropertyDescriptor(
    name: Name,
    typeDescriptor: ClassDescriptor,
    receiver: ReceiverParameterDescriptor?,
    isVar: Boolean,
    script: LazyScriptDescriptor
) : PropertyDescriptorImpl(
    script,
    null,
    Annotations.EMPTY,
    Modality.FINAL,
    Visibilities.PRIVATE,
    isVar,
    name,
    CallableMemberDescriptor.Kind.SYNTHESIZED,
    SourceElement.NO_SOURCE,
    /* lateInit = */ false, /* isConst = */ false, /* isExpect = */ false, /* isActual = */ false, /* isExternal = */ false,
    /* isDelegated = */ false
) {
    init {
        setType(typeDescriptor.defaultType, emptyList(), receiver, null)
        initialize(
            makePropertyGetterDescriptor(),
            if (!isVar) null else makePropertySetterDescriptor()
        )
    }
}

private fun PropertyDescriptorImpl.makePropertyGetterDescriptor() =
    PropertyGetterDescriptorImpl(
        this,
        Annotations.EMPTY,
        this.modality,
        this.visibility,
        /* isDefault = */
        false, /* isExternal = */
        false, /* isInline = */
        false,
        this.kind,
        null,
        SourceElement.NO_SOURCE
    ).also {
        it.initialize(returnType)
    }

private fun PropertyDescriptorImpl.makePropertySetterDescriptor() =
    PropertySetterDescriptorImpl(
        this,
        Annotations.EMPTY,
        this.modality,
        this.visibility,
        /* isDefault = */
        false, /* isExternal = */
        false, /* isInline = */
        false,
        this.kind,
        null,
        SourceElement.NO_SOURCE
    ).also {
        it.initialize(
            ValueParameterDescriptorImpl(
                this,
                null,
                0,
                Annotations.EMPTY,
                Name.special("<set-?>"),
                returnType,
                /* declaresDefaultValue = */
                false, /* isCrossinline = */
                false, /* isNoinline = */
                false,
                null,
                SourceElement.NO_SOURCE
            )
        )
    }