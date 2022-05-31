/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.sam

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl

interface SamTypeAliasConstructorDescriptor : SamConstructorDescriptor, DeclarationDescriptorWithNavigationSubstitute {
    val typeAliasDescriptor: TypeAliasDescriptor
    val expandedConstructorDescriptor: SamConstructorDescriptor

    override val substitute: DeclarationDescriptor
        get() = typeAliasDescriptor
}

class SamTypeAliasConstructorDescriptorImpl(
    override val typeAliasDescriptor: TypeAliasDescriptor,
    override val expandedConstructorDescriptor: SamConstructorDescriptor
) : SimpleFunctionDescriptorImpl(
    typeAliasDescriptor.containingDeclaration,
    null,
    expandedConstructorDescriptor.baseDescriptorForSynthetic.annotations,
    typeAliasDescriptor.name,
    CallableMemberDescriptor.Kind.SYNTHESIZED,
    typeAliasDescriptor.source
), SamTypeAliasConstructorDescriptor {
    override fun getSingleAbstractMethod(): CallableMemberDescriptor =
        expandedConstructorDescriptor.getSingleAbstractMethod()

    override val baseDescriptorForSynthetic: ClassDescriptor
        get() = expandedConstructorDescriptor.baseDescriptorForSynthetic
}
