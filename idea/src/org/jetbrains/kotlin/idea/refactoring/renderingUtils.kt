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

package org.jetbrains.kotlin.idea.refactoring

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.util.PsiFormatUtil
import com.intellij.psi.util.PsiFormatUtilBase
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

fun wrapOrSkip(s: String, inCode: Boolean) = if (inCode) "<code>$s</code>" else s

fun formatClassDescriptor(classDescriptor: DeclarationDescriptor) = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.render(classDescriptor)

fun formatPsiClass(
        psiClass: PsiClass,
        markAsJava: Boolean,
        inCode: Boolean
): String {
    var description: String

    val kind = if (psiClass.isInterface) "interface " else "class "
    description = kind + PsiFormatUtil.formatClass(
            psiClass,
            PsiFormatUtilBase.SHOW_CONTAINING_CLASS or PsiFormatUtilBase.SHOW_NAME or PsiFormatUtilBase.SHOW_PARAMETERS or PsiFormatUtilBase.SHOW_TYPE
    )
    description = wrapOrSkip(description, inCode)

    return if (markAsJava) "[Java] $description" else description
}

fun formatClass(classDescriptor: DeclarationDescriptor, inCode: Boolean): String {
    val element = DescriptorToSourceUtils.descriptorToDeclaration(classDescriptor)
    return if (element is PsiClass) {
        formatPsiClass(element, false, inCode)
    }
    else {
        wrapOrSkip(formatClassDescriptor(classDescriptor), inCode)
    }
}

fun formatFunction(functionDescriptor: DeclarationDescriptor, inCode: Boolean): String {
    val element = DescriptorToSourceUtils.descriptorToDeclaration(functionDescriptor)
    return if (element is PsiMethod) {
        formatPsiMethod(element, false, inCode)
    }
    else {
        wrapOrSkip(formatFunctionDescriptor(functionDescriptor), inCode)
    }
}

private fun formatFunctionDescriptor(functionDescriptor: DeclarationDescriptor) = DescriptorRenderer.COMPACT.render(functionDescriptor)

fun formatPsiMethod(
        psiMethod: PsiMethod,
        showContainingClass: Boolean,
        inCode: Boolean
): String {
    var options = PsiFormatUtilBase.SHOW_NAME or PsiFormatUtilBase.SHOW_PARAMETERS or PsiFormatUtilBase.SHOW_TYPE
    if (showContainingClass) {
        //noinspection ConstantConditions
        options = options or PsiFormatUtilBase.SHOW_CONTAINING_CLASS
    }

    var description = PsiFormatUtil.formatMethod(psiMethod, PsiSubstitutor.EMPTY, options, PsiFormatUtilBase.SHOW_TYPE)
    description = wrapOrSkip(description, inCode)

    return "[Java] $description"
}

fun formatJavaOrLightMethod(method: PsiMethod): String {
    val originalDeclaration = method.unwrapped
    return if (originalDeclaration is KtDeclaration) {
        formatFunctionDescriptor(originalDeclaration.unsafeResolveToDescriptor())
    }
    else {
        formatPsiMethod(method, false, false)
    }
}

fun formatClass(classOrObject: KtClassOrObject) = formatClassDescriptor(classOrObject.unsafeResolveToDescriptor() as ClassDescriptor)