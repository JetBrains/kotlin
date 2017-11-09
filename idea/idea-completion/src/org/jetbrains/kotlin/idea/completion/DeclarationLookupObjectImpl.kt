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

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.descriptorsEqualWithSubstitution

/**
 * Stores information about resolved descriptor and position of that descriptor.
 * Position will be used for sorting
 */
abstract class DeclarationLookupObjectImpl(
        final override val descriptor: DeclarationDescriptor?
): DeclarationLookupObject {
    override val name: Name?
        get() = descriptor?.name ?: (psiElement as? PsiNamedElement)?.name?.let { Name.identifier(it) }

    override val importableFqName: FqName?
        get() {
            return if (descriptor != null)
                descriptor.importableFqName
            else
                (psiElement as? PsiClass)?.qualifiedName?.let(::FqName)
        }

    override fun toString() = super.toString() + " " + (descriptor ?: psiElement)

    override fun hashCode(): Int {
        return if (descriptor != null)
            descriptor.original.hashCode()
        else
            psiElement!!.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.java != other::class.java) return false
        val lookupObject = other as DeclarationLookupObjectImpl
        return if (descriptor != null)
            descriptorsEqualWithSubstitution(descriptor, lookupObject.descriptor)
        else
            lookupObject.descriptor == null && psiElement == lookupObject.psiElement
    }

    override val isDeprecated: Boolean
        get() {
            return if (descriptor != null)
                KotlinBuiltIns.isDeprecated(descriptor)
            else
                (psiElement as? PsiDocCommentOwner)?.isDeprecated == true
        }
}
