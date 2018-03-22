/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.structureView

import com.intellij.ide.util.InheritedMembersNodeProvider
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.psi.NavigatablePsiElement
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import java.util.*

class KotlinInheritedMembersNodeProvider : InheritedMembersNodeProvider<TreeElement>() {
    override fun provideNodes(node: TreeElement): Collection<TreeElement> {
        if (node !is KotlinStructureViewElement) return listOf()

        val element = node.element as? KtClassOrObject ?: return listOf()

        val project = element.project

        val descriptor = element.resolveToDescriptorIfAny(BodyResolveMode.FULL) ?: return listOf()

        val children = ArrayList<TreeElement>()

        val defaultType = descriptor.defaultType
        for (memberDescriptor in defaultType.memberScope.getContributedDescriptors()) {
            if (memberDescriptor !is CallableMemberDescriptor) continue

            when (memberDescriptor.kind) {
                CallableMemberDescriptor.Kind.FAKE_OVERRIDE,
                CallableMemberDescriptor.Kind.DELEGATION -> {
                    val superTypeMember = DescriptorToSourceUtilsIde.getAnyDeclaration(project, memberDescriptor)
                    if (superTypeMember is NavigatablePsiElement) {
                        children.add(KotlinStructureViewElement(superTypeMember, memberDescriptor, true))
                    }
                }
                CallableMemberDescriptor.Kind.DECLARATION -> Unit /* Don't show */
                CallableMemberDescriptor.Kind.SYNTHESIZED -> Unit /* Don't show */
            }
        }

        return children
    }
}
