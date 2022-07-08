/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fileClasses

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor

fun DeclarationDescriptor.isTopLevelInJvmMultifileClass(): Boolean {
    if (containingDeclaration !is PackageFragmentDescriptor) return false

    val declaration = DescriptorToSourceUtils.descriptorToDeclaration(this)
    if (declaration is KtDeclaration) {
        return declaration.isInsideJvmMultifileClassFile()
    }

    if (this is DeserializedMemberDescriptor) {
        val containerSource = containerSource
        if (containerSource is JvmPackagePartSource && containerSource.facadeClassName != null) {
            return true
        }
    }

    return false
}