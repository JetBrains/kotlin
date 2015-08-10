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

package org.jetbrains.kotlin.idea.refactoring.memberInfo

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.getJavaClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext

fun PsiNamedElement.getClassDescriptorIfAny(resolutionFacade: ResolutionFacade? = null): ClassDescriptor? {
    return when (this) {
        is JetClass -> (resolutionFacade ?: getResolutionFacade()).resolveToDescriptor(this) as ClassDescriptor
        is PsiClass -> getJavaClassDescriptor()
        else -> null
    }
}

fun PsiNamedElement.isAbstractMember(): Boolean {
    return when(this) {
        is JetNamedDeclaration -> hasModifier(JetTokens.ABSTRACT_KEYWORD)
        is PsiMember -> hasModifierProperty(PsiModifier.ABSTRACT)
        else -> false
    }
}

// Applies to JetClassOrObject and PsiClass
public fun PsiNamedElement.qualifiedClassNameForRendering(): String {
    val fqName = when (this) {
        is JetClassOrObject -> fqName?.asString()
        is PsiClass -> qualifiedName
        else -> throw AssertionError("Not a class: ${getElementTextWithContext()}")
    }
    return fqName ?: name ?: "[Anonymous]"
}