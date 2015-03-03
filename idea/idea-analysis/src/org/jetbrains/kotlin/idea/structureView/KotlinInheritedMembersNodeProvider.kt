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

package org.jetbrains.kotlin.idea.structureView

import com.intellij.ide.util.InheritedMembersNodeProvider
import com.intellij.ide.util.treeView.smartTree.TreeElement
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.resolve.BindingContext
import java.util.ArrayList
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import com.intellij.psi.NavigatablePsiElement
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.idea.caches.resolve.analyze

public class KotlinInheritedMembersNodeProvider: InheritedMembersNodeProvider<TreeElement>() {
    override fun provideNodes(node: TreeElement): Collection<TreeElement> {
        if (node !is JetStructureViewElement) return listOf()

        val element = node.getElement()
        if (element !is JetClassOrObject) return listOf()

        [suppress("USELESS_CAST")] // KT-3996 Workaround
        val project = (element as NavigatablePsiElement).getProject()

        val context = element.analyze()
        val descriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, element]

        if (descriptor !is ClassifierDescriptor) return listOf()

        val children = ArrayList<TreeElement>()

        val defaultType = descriptor.getDefaultType()
        for (memberDescriptor in defaultType.getMemberScope().getDescriptors()) {
            if (memberDescriptor !is CallableMemberDescriptor) continue

            when (memberDescriptor.getKind()) {
                CallableMemberDescriptor.Kind.FAKE_OVERRIDE,
                CallableMemberDescriptor.Kind.DELEGATION -> {
                    val superTypeMember = DescriptorToSourceUtilsIde.getAnyDeclaration(project, memberDescriptor)
                    if (superTypeMember is NavigatablePsiElement) {
                        children.add(JetStructureViewElement(superTypeMember, memberDescriptor, true))
                    }
                }
            }
        }

        return children
    }
}
