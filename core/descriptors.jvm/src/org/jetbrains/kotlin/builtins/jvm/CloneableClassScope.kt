/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins.jvm

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DECLARATION
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.GivenFunctionsMemberScope
import org.jetbrains.kotlin.storage.StorageManager

class CloneableClassScope(
    storageManager: StorageManager,
    containingClass: ClassDescriptor
) : GivenFunctionsMemberScope(storageManager, containingClass) {
    override fun computeDeclaredFunctions(): List<FunctionDescriptor> = listOf(
        SimpleFunctionDescriptorImpl.create(containingClass, Annotations.EMPTY, CLONE_NAME, DECLARATION, SourceElement.NO_SOURCE).apply {
            initialize(
                null, containingClass.thisAsReceiverParameter, emptyList(), emptyList(), emptyList(), containingClass.builtIns.anyType,
                Modality.OPEN, DescriptorVisibilities.PROTECTED
            )
        }
    )

    companion object {
        val CLONE_NAME = Name.identifier("clone")
    }
}
