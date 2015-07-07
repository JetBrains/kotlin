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

import com.intellij.refactoring.classMembers.MemberInfoBase
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetNamedDeclaration

public class KotlinMemberInfo(member: JetNamedDeclaration) : MemberInfoBase<JetNamedDeclaration>(member) {
    init {
        val memberDescriptor = member.resolveToDescriptor()
        isStatic = member.getParent() is JetFile
        displayName = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.render(memberDescriptor)

        val overriddenDescriptors = (memberDescriptor as? CallableMemberDescriptor)?.getOverriddenDescriptors() ?: emptySet()
        if (overriddenDescriptors.isNotEmpty()) {
            overrides = overriddenDescriptors.any { it.getModality() != Modality.ABSTRACT }
        }
    }
}
