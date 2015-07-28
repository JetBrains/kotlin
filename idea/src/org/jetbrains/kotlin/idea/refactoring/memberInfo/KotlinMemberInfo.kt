/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.memberInfo

import com.intellij.psi.PsiMember
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.classMembers.MemberInfoBase
import com.intellij.refactoring.util.classMembers.MemberInfo
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptySet

public class KotlinMemberInfo(member: JetNamedDeclaration, val isSuperClass: Boolean = false) : MemberInfoBase<JetNamedDeclaration>(member) {
    companion object {
        private val RENDERER = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.withOptions {
            modifiers = DescriptorRendererModifier.INNER.singletonOrEmptySet()
        }
    }

    init {
        val memberDescriptor = member.resolveToDescriptor()
        isStatic = member.parent is JetFile

        if (member is JetClass && isSuperClass) {
            if (member.isInterface()) {
                displayName = RefactoringBundle.message("member.info.implements.0", member.name)
                overrides = false
            }
            else {
                displayName = RefactoringBundle.message("member.info.extends.0", member.name)
                overrides = true
            }
        }
        else {
            displayName = RENDERER.render(memberDescriptor)
            if (memberDescriptor is MemberDescriptor && memberDescriptor.modality == Modality.ABSTRACT) {
                displayName = "abstract $displayName"
            }

            val overriddenDescriptors = (memberDescriptor as? CallableMemberDescriptor)?.overriddenDescriptors ?: emptySet()
            if (overriddenDescriptors.isNotEmpty()) {
                overrides = overriddenDescriptors.any { it.modality != Modality.ABSTRACT }
            }
        }
    }
}

public fun KotlinMemberInfo.toJavaMemberInfo(): MemberInfo? {
    val declaration = member
    val psiMember: PsiMember? = when (declaration) {
        is JetNamedFunction, is JetProperty -> declaration.getRepresentativeLightMethod()
        is JetClassOrObject -> declaration.toLightClass()
        else -> null
    }
    if (psiMember == null) return null

    val info = MemberInfo(psiMember, isSuperClass, null)
    info.isToAbstract = isToAbstract
    return info
}