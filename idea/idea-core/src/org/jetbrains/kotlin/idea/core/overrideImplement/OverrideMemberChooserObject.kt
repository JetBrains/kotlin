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

package org.jetbrains.kotlin.idea.core.overrideImplement

import com.intellij.codeInsight.generation.ClassMember
import com.intellij.codeInsight.generation.MemberChooserObject
import com.intellij.codeInsight.generation.MemberChooserObjectBase
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.util.DescriptorMemberChooserObject

interface OverrideMemberChooserObject : ClassMember {
    val descriptor: CallableMemberDescriptor
    val immediateSuper: CallableMemberDescriptor

    companion object {
        fun create(project: Project, descriptor: CallableMemberDescriptor, immediateSuper: CallableMemberDescriptor): OverrideMemberChooserObject {
            val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor)
            if (declaration != null) {
                return WithDeclaration(descriptor, declaration, immediateSuper)
            }
            else {
                return WithoutDeclaration(descriptor, immediateSuper)
            }
        }

        private class WithDeclaration(
                descriptor: CallableMemberDescriptor,
                declaration: PsiElement,
                override val immediateSuper: CallableMemberDescriptor
        ) : DescriptorMemberChooserObject(declaration, descriptor), OverrideMemberChooserObject {

            override val descriptor: CallableMemberDescriptor
                get() = super<DescriptorMemberChooserObject>.descriptor as CallableMemberDescriptor
        }

        private class WithoutDeclaration(
                override val descriptor: CallableMemberDescriptor,
                override val immediateSuper: CallableMemberDescriptor
        ) : MemberChooserObjectBase(DescriptorMemberChooserObject.getText(descriptor), DescriptorMemberChooserObject.getIcon(null, descriptor)), OverrideMemberChooserObject {

            override fun getParentNodeDelegate(): MemberChooserObject? {
                val parentClassifier = descriptor.containingDeclaration as? ClassifierDescriptor ?: return null
                return MemberChooserObjectBase(DescriptorMemberChooserObject.getText(parentClassifier), DescriptorMemberChooserObject.getIcon(null, parentClassifier))
            }
        }
    }
}
