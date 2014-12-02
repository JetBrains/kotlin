/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.completion

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.plugin.caches.resolve.ResolutionFacade
import org.jetbrains.jet.lang.descriptors.CallableDescriptor

/**
 * Stores information about resolved descriptor and position of that descriptor.
 * Position will be used for sorting
 */
public class DeclarationDescriptorLookupObject(
        public val descriptor: DeclarationDescriptor,
        private val resolutionFacade: ResolutionFacade,
        public val psiElement: PsiElement?
) {
    override fun toString(): String {
        return super.toString() + " " + descriptor
    }

    override fun hashCode(): Int {
        return descriptor.getOriginal().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this identityEquals other) return true
        if (other == null || javaClass != other.javaClass) return false

        val lookupObject = other as DeclarationDescriptorLookupObject

        if (resolutionFacade != lookupObject.resolutionFacade) {
            LOG.warn("Descriptors from different resolve sessions")
            return false
        }

        if (lookupObject.descriptor.getOriginal() != descriptor.getOriginal()) return false
        if (descriptor !is CallableDescriptor) return true
        // optimization:
        if (descriptor == (descriptor as CallableDescriptor).getOriginal() && lookupObject.descriptor == lookupObject.descriptor.getOriginal()) return true
        return substitutionsEqual(descriptor as CallableDescriptor, lookupObject.descriptor as CallableDescriptor)
    }

    private fun substitutionsEqual(callable1: CallableDescriptor, callable2: CallableDescriptor): Boolean {
        if (callable1.getReturnType() != callable2.getReturnType()) return false
        val parameters1 = callable1.getValueParameters()
        val parameters2 = callable2.getValueParameters()
        if (parameters1.size() != parameters2.size()) return false
        for (i in parameters1.indices) {
            if (parameters1[i].getType() != parameters2[i].getType()) return false
        }
        return true
    }

    class object {
        private val LOG = Logger.getInstance("#" + javaClass<DeclarationDescriptorLookupObject>().getName())
    }
}
