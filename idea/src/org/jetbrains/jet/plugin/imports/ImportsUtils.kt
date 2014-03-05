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

package org.jetbrains.jet.plugin.imports

import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import com.intellij.psi.PsiElement

public val DeclarationDescriptor.importableFqName: FqName?
    get() {
        if (this is ConstructorDescriptor) return getContainingDeclaration().importableFqName
        val mayBeUnsafe = DescriptorUtils.getFqName(this)
        return if (mayBeUnsafe.isSafe()) {
            mayBeUnsafe.toSafe()
        }
        else {
            null
        }
    }

public fun DeclarationDescriptor.canBeReferencedViaImport(): Boolean {
    if (this is PackageViewDescriptor || DescriptorUtils.isTopLevelDeclaration(this)) {
        return true
    }
    val parent = getContainingDeclaration()!!
    if (parent !is ClassDescriptor || !parent.canBeReferencedViaImport()) {
        return false
    }
    // inner class constructors can't be referenced via import
    if (this is ConstructorDescriptor && parent.isInner()) {
        return false
    }
    return this is ClassDescriptor || this is ConstructorDescriptor
}

public fun isInReceiverScope(referenceElement: PsiElement, referencedDescriptor: DeclarationDescriptor): Boolean {
    val isExpressionWithReceiver = referenceElement is JetSimpleNameExpression && referenceElement.getReceiverExpression() != null
    return isExpressionWithReceiver && !referencedDescriptor.isExtension
}

//TODO: move this utility to more appropriate place
public val DeclarationDescriptor.isExtension: Boolean
    get() = this is CallableDescriptor && getReceiverParameter() != null