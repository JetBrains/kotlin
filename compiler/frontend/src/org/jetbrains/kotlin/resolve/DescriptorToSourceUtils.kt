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

package org.jetbrains.kotlin.resolve

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DECLARATION
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.synthetic.SyntheticMemberDescriptor
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.source.getPsi
import java.util.*

object DescriptorToSourceUtils {
    private fun collectEffectiveReferencedDescriptors(result: MutableList<DeclarationDescriptor>, descriptor: DeclarationDescriptor) {
        if (descriptor is DeclarationDescriptorWithNavigationSubstitute) {
            collectEffectiveReferencedDescriptors(result, descriptor.substitute)
            return
        }

        if (descriptor is CallableMemberDescriptor) {
            val kind = descriptor.kind
            if (kind != DECLARATION && kind != SYNTHESIZED) {
                for (overridden in descriptor.overriddenDescriptors) {
                    collectEffectiveReferencedDescriptors(result, overridden.original)
                }
                return
            }
            if (descriptor is SyntheticMemberDescriptor<*>) {
                collectEffectiveReferencedDescriptors(result, descriptor.baseDescriptorForSynthetic)
                return
            }
        }
        result.add(descriptor)
    }

    @JvmStatic
    fun getEffectiveReferencedDescriptors(descriptor: DeclarationDescriptor): Collection<DeclarationDescriptor> {
        val result = ArrayList<DeclarationDescriptor>()
        collectEffectiveReferencedDescriptors(result, descriptor.original)
        return result
    }

    // TODO Fix in descriptor
    @JvmStatic
    private fun getSourceForExtensionReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor): PsiElement? {
        // Only for extension receivers
        if (descriptor.source != SourceElement.NO_SOURCE || descriptor.value !is ExtensionReceiver) return null
        val containingDeclaration = descriptor.containingDeclaration as? CallableDescriptor ?: return null
        val psi = containingDeclaration.source.getPsi() as? KtCallableDeclaration ?: return null
        return psi.receiverTypeReference
    }

    @JvmStatic
    fun getSourceFromDescriptor(descriptor: DeclarationDescriptor): PsiElement? {
        if (descriptor is ReceiverParameterDescriptor) {
            getSourceForExtensionReceiverParameterDescriptor(descriptor)?.let { return it }
        }

        return (descriptor as? DeclarationDescriptorWithSource)?.source?.getPsi()
    }

    @JvmStatic
    fun getSourceFromAnnotation(descriptor: AnnotationDescriptor): KtAnnotationEntry? {
        return descriptor.source.getPsi() as? KtAnnotationEntry
    }

    // NOTE this is also used by KDoc
    // Returns PSI element for descriptor. If there are many relevant elements (e.g. it is fake override
    // with multiple declarations), returns null. It can't find declarations in builtins or decompiled code.
    // In IDE, use DescriptorToSourceUtilsIde instead.
    @JvmStatic
    fun descriptorToDeclaration(descriptor: DeclarationDescriptor): PsiElement? {
        val effectiveReferencedDescriptors = getEffectiveReferencedDescriptors(descriptor)
        return if (effectiveReferencedDescriptors.size == 1) getSourceFromDescriptor(effectiveReferencedDescriptors.firstOrNull()!!) else null
    }

    @JvmStatic
    fun getContainingFile(declarationDescriptor: DeclarationDescriptor): KtFile? {
        // declarationDescriptor may describe a synthesized element which doesn't have PSI
        // To workaround that, we find a top-level parent (which is inside a PackageFragmentDescriptor), which is guaranteed to have PSI
        val descriptor = findTopLevelParent(declarationDescriptor) ?: return null

        val declaration = descriptorToDeclaration(descriptor) ?: return null

        return declaration.containingFile as? KtFile
    }

    private fun findTopLevelParent(declarationDescriptor: DeclarationDescriptor): DeclarationDescriptor? {
        var descriptor: DeclarationDescriptor? = declarationDescriptor
        if (declarationDescriptor is PropertyAccessorDescriptor) {
            descriptor = (descriptor as PropertyAccessorDescriptor).correspondingProperty
        }
        while (!(descriptor == null || DescriptorUtils.isTopLevelDeclaration(descriptor))) {
            descriptor = descriptor.containingDeclaration
        }
        return descriptor
    }
}
