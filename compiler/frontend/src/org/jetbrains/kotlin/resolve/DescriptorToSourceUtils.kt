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

package org.jetbrains.kotlin.resolve

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.source.*

import java.util.ArrayList

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DECLARATION
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED
import kotlin.platform.*

public object DescriptorToSourceUtils {
    private fun collectEffectiveReferencedDescriptors(result: MutableList<DeclarationDescriptor>, descriptor: DeclarationDescriptor) {
        if (descriptor is CallableMemberDescriptor) {
            val kind = descriptor.getKind()
            if (kind != DECLARATION && kind != SYNTHESIZED) {
                for (overridden in descriptor.getOverriddenDescriptors()) {
                    collectEffectiveReferencedDescriptors(result, overridden.getOriginal())
                }
                return
            }
        }
        result.add(descriptor)
    }

    platformStatic
    public fun getEffectiveReferencedDescriptors(descriptor: DeclarationDescriptor): Collection<DeclarationDescriptor> {
        val result = ArrayList<DeclarationDescriptor>()
        collectEffectiveReferencedDescriptors(result, descriptor.getOriginal())
        return result
    }

    platformStatic
    public fun getSourceFromDescriptor(descriptor: DeclarationDescriptor): PsiElement? {
        return (descriptor as? DeclarationDescriptorWithSource)?.getSource()?.getPsi()
    }

    // NOTE this is also used by KDoc
    // Returns PSI element for descriptor. If there are many relevant elements (e.g. it is fake override
    // with multiple declarations), returns null. It can't find declarations in builtins or decompiled code.
    // In IDE, use DescriptorToSourceUtilsIde instead.
    platformStatic
    public fun descriptorToDeclaration(descriptor: DeclarationDescriptor): PsiElement? {
        val effectiveReferencedDescriptors = getEffectiveReferencedDescriptors(descriptor)
        return if (effectiveReferencedDescriptors.size() == 1) getSourceFromDescriptor(effectiveReferencedDescriptors.firstOrNull()!!) else null
    }

    platformStatic
    public fun getContainingFile(declarationDescriptor: DeclarationDescriptor): JetFile? {
        // declarationDescriptor may describe a synthesized element which doesn't have PSI
        // To workaround that, we find a top-level parent (which is inside a PackageFragmentDescriptor), which is guaranteed to have PSI
        val descriptor = findTopLevelParent(declarationDescriptor) ?: return null

        val declaration = descriptorToDeclaration(descriptor) ?: return null

        return declaration.getContainingFile() as? JetFile
    }

    private fun findTopLevelParent(declarationDescriptor: DeclarationDescriptor): DeclarationDescriptor? {
        var descriptor: DeclarationDescriptor? = declarationDescriptor
        if (declarationDescriptor is PropertyAccessorDescriptor) {
            descriptor = (descriptor as PropertyAccessorDescriptor).getCorrespondingProperty()
        }
        while (!(descriptor == null || DescriptorUtils.isTopLevelDeclaration(descriptor!!))) {
            descriptor = descriptor!!.getContainingDeclaration()
        }
        return descriptor
    }
}
