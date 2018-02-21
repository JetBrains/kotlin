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

package org.jetbrains.kotlin.idea.refactoring.safeDelete

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceSimpleDeleteUsageInfo
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaMemberDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.targetDescriptors
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class SafeDeleteImportDirectiveUsageInfo(
        importDirective: KtImportDirective, declaration: PsiElement
) : SafeDeleteReferenceSimpleDeleteUsageInfo(importDirective, declaration, importDirective.isSafeToDelete(declaration))

private fun KtImportDirective.isSafeToDelete(element: PsiElement): Boolean {
    val referencedDescriptor = targetDescriptors().singleOrNull() ?: return false
    val unwrappedElement = element.unwrapped
    val declarationDescriptor = when (unwrappedElement) {
        is KtDeclaration -> unwrappedElement.resolveToDescriptorIfAny(BodyResolveMode.FULL)
        is PsiMember -> unwrappedElement.getJavaMemberDescriptor()
        else -> return false
    }
    return referencedDescriptor == declarationDescriptor
}
