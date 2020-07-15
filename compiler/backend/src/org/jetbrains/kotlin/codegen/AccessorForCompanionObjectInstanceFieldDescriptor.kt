/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.name.Name

class AccessorForCompanionObjectInstanceFieldDescriptor(
    val companionObjectDescriptor: ClassDescriptor,
    name: Name
) :
    SimpleFunctionDescriptorImpl(
        companionObjectDescriptor.containingDeclaration,
        null, Annotations.EMPTY,
        name,
        CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE
    ) {

    init {
        initialize(
            null, null, emptyList(), emptyList(), emptyList(),
            companionObjectDescriptor.defaultType,
            Modality.FINAL,
            DescriptorVisibilities.LOCAL
        )
    }

    override fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        original: FunctionDescriptor?,
        kind: CallableMemberDescriptor.Kind,
        newName: Name?,
        annotations: Annotations,
        source: SourceElement
    ): FunctionDescriptorImpl {
        throw UnsupportedOperationException("Accessor for companion object $companionObjectDescriptor should not be substituted")
    }
}
