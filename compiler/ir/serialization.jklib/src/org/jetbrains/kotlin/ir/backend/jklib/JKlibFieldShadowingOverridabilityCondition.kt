/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.jklib

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition
import org.jetbrains.kotlin.ir.overrides.MemberWithOriginal
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageFragment

/**
 * Custom overridability condition for JKlib compilation. Prevents fake override generation when
 * a Kotlin property shadows a Java superclass field of the same name by returning OVERRIDABLE.
 * Returning OVERRIDABLE suppresses duplicate fake override property creation without symbol collisions.
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
object JKlibFieldShadowingOverridabilityCondition : IrExternalOverridabilityCondition {
    override val contract: IrExternalOverridabilityCondition.Contract
        get() = IrExternalOverridabilityCondition.Contract.SUCCESS_ONLY

    override fun isOverridable(
        superMember: MemberWithOriginal,
        subMember: MemberWithOriginal,
    ): IrExternalOverridabilityCondition.Result {
        val superProperty =
            superMember.original as? IrProperty ?: return IrExternalOverridabilityCondition.Result.UNKNOWN
        val subProperty =
            subMember.original as? IrProperty ?: return IrExternalOverridabilityCondition.Result.UNKNOWN

        // 1. Names must match
        if (superProperty.name != subProperty.name) {
            return IrExternalOverridabilityCondition.Result.UNKNOWN
        }

        // 2. Do not match extension properties
        if (superProperty.getter?.extensionReceiverParameter != null ||
            subProperty.getter?.extensionReceiverParameter != null) {
            return IrExternalOverridabilityCondition.Result.UNKNOWN
        }

        // 3. Target field shadowing: super is Java member AND at least one member lacks accessors
        val isFieldShadowing = superProperty.descriptor.isJavaDescriptor() &&
            (superProperty.getter == null || subProperty.getter == null)

        if (isFieldShadowing) {
            // OVERRIDABLE suppresses fake override generation
            return IrExternalOverridabilityCondition.Result.OVERRIDABLE
        }

        return IrExternalOverridabilityCondition.Result.UNKNOWN
    }

    private fun DeclarationDescriptor.isJavaDescriptor(): Boolean {
        if (this is PackageFragmentDescriptor) {
            return this is LazyJavaPackageFragment
        }

        return this is JavaClassDescriptor ||
            this is JavaCallableMemberDescriptor ||
            (containingDeclaration?.isJavaDescriptor() == true)
    }
}
