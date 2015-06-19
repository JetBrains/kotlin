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

package org.jetbrains.kotlin.idea.completion

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject
import javax.swing.Icon

/**
 * Stores information about resolved descriptor and position of that descriptor.
 * Position will be used for sorting
 */
public abstract class DeclarationLookupObjectImpl(
        public final override val descriptor: DeclarationDescriptor?,
        public final override val psiElement: PsiElement?,
        private val resolutionFacade: ResolutionFacade
): DeclarationLookupObject {
    init {
        assert(descriptor != null || psiElement != null)
    }

    override fun toString() = super<DeclarationLookupObject>.toString() + " " + (descriptor ?: psiElement)

    override fun hashCode(): Int {
        return if (descriptor != null) descriptor.getOriginal().hashCode() else psiElement!!.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this identityEquals other) return true
        if (other == null || javaClass != other.javaClass) return false

        val lookupObject = other as DeclarationLookupObjectImpl

        if (resolutionFacade != lookupObject.resolutionFacade) {
            LOG.warn("Descriptors from different resolve sessions")
            return false
        }

        return descriptorsEqualWithSubstitution(descriptor, lookupObject.descriptor) && psiElement == lookupObject.psiElement
    }

    override val isDeprecated = if (descriptor != null) KotlinBuiltIns.isDeprecated(descriptor) else (psiElement as PsiDocCommentOwner).isDeprecated()

    companion object {
        private val LOG = Logger.getInstance("#" + javaClass<DeclarationLookupObject>().getName())
    }
}
