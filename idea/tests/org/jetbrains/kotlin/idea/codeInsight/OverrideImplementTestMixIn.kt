/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.codeInsight.generation.ClassMember
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.core.overrideImplement.AbstractGenerateMembersHandler
import org.jetbrains.kotlin.idea.core.overrideImplement.ImplementMembersHandler
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMemberChooserObject
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMembersHandler
import org.jetbrains.kotlin.psi.KtClassOrObject

interface OverrideImplementTestMixIn<T : ClassMember> {
    fun createImplementMembersHandler(): AbstractGenerateMembersHandler<T>
    fun createOverrideMembersHandler(): AbstractGenerateMembersHandler<T>
    fun isMemberOfAny(parentClass: KtClassOrObject, chooserObject: T): Boolean
    fun getMemberName(parentClass: KtClassOrObject, chooserObject: T): String
    fun getContainingClassName(parentClass: KtClassOrObject, chooserObject: T): String
}

interface OldOverrideImplementTestMixIn : OverrideImplementTestMixIn<OverrideMemberChooserObject> {
    override fun createImplementMembersHandler(): AbstractGenerateMembersHandler<OverrideMemberChooserObject> = ImplementMembersHandler()

    override fun createOverrideMembersHandler(): AbstractGenerateMembersHandler<OverrideMemberChooserObject> = OverrideMembersHandler()

    override fun isMemberOfAny(parentClass: KtClassOrObject, chooserObject: OverrideMemberChooserObject): Boolean =
        (chooserObject.descriptor.containingDeclaration as? ClassDescriptor)?.let {
            KotlinBuiltIns.isAny(it)
        } ?: true

    override fun getMemberName(parentClass: KtClassOrObject, chooserObject: OverrideMemberChooserObject): String =
        chooserObject.descriptor.name.asString()

    override fun getContainingClassName(parentClass: KtClassOrObject, chooserObject: OverrideMemberChooserObject): String {
        return chooserObject.immediateSuper.containingDeclaration.name.asString()
    }
}