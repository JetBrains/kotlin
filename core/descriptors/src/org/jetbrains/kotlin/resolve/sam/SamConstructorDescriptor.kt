/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.sam

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.synthetic.FunctionInterfaceConstructorDescriptor
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindExclude

interface SamConstructorDescriptor : SimpleFunctionDescriptor, FunctionInterfaceConstructorDescriptor {
    fun getSingleAbstractMethod(): CallableMemberDescriptor
}

class SamConstructorDescriptorImpl(
    containingDeclaration: DeclarationDescriptor,
    private val samInterface: ClassDescriptor
) : SimpleFunctionDescriptorImpl(
    containingDeclaration,
    null,
    samInterface.annotations,
    samInterface.name,
    CallableMemberDescriptor.Kind.SYNTHESIZED,
    samInterface.source
), SamConstructorDescriptor {
    override val baseDescriptorForSynthetic: ClassDescriptor
        get() = samInterface

    override fun getSingleAbstractMethod(): CallableMemberDescriptor =
        getAbstractMembers(samInterface).single()
}

object SamConstructorDescriptorKindExclude : DescriptorKindExclude() {
    override fun excludes(descriptor: DeclarationDescriptor) = descriptor is SamConstructorDescriptor

    override val fullyExcludedDescriptorKinds: Int get() = 0
}
