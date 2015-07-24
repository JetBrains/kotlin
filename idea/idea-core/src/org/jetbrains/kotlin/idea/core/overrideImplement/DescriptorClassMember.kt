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

import com.intellij.codeInsight.generation.ClassMemberWithElement
import com.intellij.codeInsight.generation.MemberChooserObject
import com.intellij.codeInsight.generation.MemberChooserObjectBase
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
import org.jetbrains.kotlin.resolve.DescriptorUtils
import javax.swing.Icon

public class DescriptorClassMember(private val myPsiElement: PsiElement, public val descriptor: DeclarationDescriptor) : MemberChooserObjectBase(DescriptorClassMember.getText(descriptor), DescriptorClassMember.getIcon(myPsiElement, descriptor)), ClassMemberWithElement {

    override fun getParentNodeDelegate(): MemberChooserObject {
        val parent = descriptor.containingDeclaration
        var declaration: PsiElement?
        if (myPsiElement is JetDeclaration) {
            // kotlin
            declaration = PsiTreeUtil.getStubOrPsiParentOfType(myPsiElement, javaClass<JetNamedDeclaration>())
            if (declaration == null) {
                declaration = PsiTreeUtil.getStubOrPsiParentOfType(myPsiElement, javaClass<JetFile>())
            }
        }
        else {
            // java or bytecode
            declaration = (myPsiElement as PsiMember).containingClass
        }
        assert(parent != null) { "$NO_PARENT_FOR$descriptor" }
        assert(declaration != null) { "$NO_PARENT_FOR$myPsiElement" }

        if (declaration is JetFile) {
            return PsiElementMemberChooserObject(declaration, declaration.name)
        }

        return DescriptorClassMember(declaration!!, parent!!)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as DescriptorClassMember

        if (descriptor != that.descriptor) return false

        return true
    }

    override fun hashCode(): Int {
        return descriptor.hashCode()
    }

    override fun getElement(): PsiElement {
        return myPsiElement
    }

    companion object {

        public val NO_PARENT_FOR: String = "No parent for "

        private val MEMBER_RENDERER = DescriptorRenderer.withOptions {
            withDefinedIn = false
            modifiers = emptySet<DescriptorRendererModifier>()
            startFromName = true
            nameShortness = NameShortness.SHORT
        }

        private fun getText(descriptor: DeclarationDescriptor): String {
            if (descriptor is ClassDescriptor) {
                return DescriptorUtils.getFqNameSafe(descriptor).render()
            }
            else {
                return MEMBER_RENDERER.render(descriptor)
            }
        }

        private fun getIcon(element: PsiElement, declarationDescriptor: DeclarationDescriptor): Icon {
            if (element.isValid) {
                val isClass = element is PsiClass || element is JetClass
                val flags = if (isClass) 0 else Iconable.ICON_FLAG_VISIBILITY
                if (element is JetDeclaration) {
                    // kotlin declaration
                    // visibility and abstraction better detect by a descriptor
                    return JetDescriptorIconProvider.getIcon(declarationDescriptor, element, flags)
                }
                else {
                    // it is better to show java icons for java code
                    return element.getIcon(flags)
                }
            }

            return JetDescriptorIconProvider.getIcon(declarationDescriptor, element, 0)
        }
    }
}
