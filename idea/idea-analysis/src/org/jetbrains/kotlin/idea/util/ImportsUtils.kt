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

package org.jetbrains.kotlin.idea.imports

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*

public val DeclarationDescriptor.importableFqName: FqName?
    get() {
        val mayBeUnsafe = DescriptorUtils.getFqName(getImportableDescriptor())
        return if (mayBeUnsafe.isSafe()) mayBeUnsafe.toSafe() else null
    }

public val DeclarationDescriptor.importableFqNameSafe: FqName
    get() = DescriptorUtils.getFqNameSafe(getImportableDescriptor())

public fun DeclarationDescriptor.canBeReferencedViaImport(): Boolean {
    if (this is PackageViewDescriptor ||
        DescriptorUtils.isTopLevelDeclaration(this) ||
        (this is CallableDescriptor && DescriptorUtils.isStaticDeclaration(this))) {
        return !getName().isSpecial()
    }

    val parentClass = getContainingDeclaration() as? ClassDescriptor ?: return false
    if (!parentClass.canBeReferencedViaImport()) return false

    return when (this) {
        is ConstructorDescriptor -> !parentClass.isInner() // inner class constructors can't be referenced via import
        is ClassDescriptor -> true
        else -> false
    }
}

public fun JetType.canBeReferencedViaImport(): Boolean {
    val descriptor = getConstructor().getDeclarationDescriptor()
    return descriptor != null && descriptor.canBeReferencedViaImport()
}

// for cases when class qualifier refers default object treats it like reference to class itself
public fun JetReferenceExpression.getImportableTargets(bindingContext: BindingContext): Collection<DeclarationDescriptor> {
    val targets = bindingContext[BindingContext.SHORT_REFERENCE_TO_DEFAULT_OBJECT, this]?.let { listOf(it) }
                     ?: bindingContext[BindingContext.REFERENCE_TARGET, this]?.let { listOf(it) }
                     ?: bindingContext[BindingContext.AMBIGUOUS_REFERENCE_TARGET, this]
                     ?: listOf()
    return targets.map { it.getImportableDescriptor() }.toSet()
}
