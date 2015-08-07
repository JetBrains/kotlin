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

package org.jetbrains.kotlin.idea.core.util

import com.intellij.codeInsight.generation.ClassMemberWithElement
import com.intellij.codeInsight.generation.MemberChooserObject
import com.intellij.codeInsight.generation.PsiElementMemberChooserObject
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.JetDescriptorIconProvider
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.renderer.NameShortness
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import javax.swing.Icon

public open class DescriptorMemberChooserObject(
        psiElement: PsiElement,
        public open val descriptor: DeclarationDescriptor
) : PsiElementMemberChooserObject(psiElement, DescriptorMemberChooserObject.getText(descriptor), DescriptorMemberChooserObject.getIcon(psiElement, descriptor)), ClassMemberWithElement {

    override fun getParentNodeDelegate(): MemberChooserObject {
        val parent = descriptor.containingDeclaration ?: error("No parent for $descriptor")

        val declaration = if (psiElement is JetDeclaration) { // kotlin
            PsiTreeUtil.getStubOrPsiParentOfType(psiElement, javaClass<JetNamedDeclaration>())
                ?: PsiTreeUtil.getStubOrPsiParentOfType(psiElement, javaClass<JetFile>())
        }
        else { // java or compiled
            (psiElement as PsiMember).containingClass
        } ?: error("No parent for $psiElement")

        return when (declaration) {
            is JetFile -> PsiElementMemberChooserObject(declaration, declaration.name)
            else -> DescriptorMemberChooserObject(declaration, parent)
        }
    }

    override fun equals(other: Any?) = this === other || other is DescriptorMemberChooserObject && descriptor == other.descriptor

    override fun hashCode() = descriptor.hashCode()

    override fun getElement() = psiElement

    companion object {
        private val MEMBER_RENDERER = DescriptorRenderer.withOptions {
            withDefinedIn = false
            modifiers = emptySet<DescriptorRendererModifier>()
            startFromName = true
            nameShortness = NameShortness.SHORT
        }

        public fun getText(descriptor: DeclarationDescriptor): String {
            return if (descriptor is ClassDescriptor)
                descriptor.fqNameSafe.render()
            else
                MEMBER_RENDERER.render(descriptor)
        }

        public fun getIcon(declaration: PsiElement?, descriptor: DeclarationDescriptor): Icon {
            if (declaration != null && declaration.isValid) {
                val isClass = declaration is PsiClass || declaration is JetClass
                val flags = if (isClass) 0 else Iconable.ICON_FLAG_VISIBILITY
                if (declaration is JetDeclaration) {
                    // kotlin declaration
                    // visibility and abstraction better detect by a descriptor
                    return JetDescriptorIconProvider.getIcon(descriptor, declaration, flags)
                }
                else {
                    // it is better to show java icons for java code
                    return declaration.getIcon(flags)
                }
            }
            else {
                return JetDescriptorIconProvider.getIcon(descriptor, declaration, 0)
            }
        }
    }
}
