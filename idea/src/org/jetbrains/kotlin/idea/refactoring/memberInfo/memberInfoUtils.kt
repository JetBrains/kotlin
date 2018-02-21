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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.util.javaResolutionFacade
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

fun PsiNamedElement.getClassDescriptorIfAny(resolutionFacade: ResolutionFacade? = null): ClassDescriptor? {
    return when (this) {
        is KtClassOrObject -> resolutionFacade?.resolveToDescriptor(this) ?: resolveToDescriptorIfAny(BodyResolveMode.FULL)
        is PsiClass -> getJavaClassDescriptor()
        else -> null
    } as? ClassDescriptor
}

// Applies to JetClassOrObject and PsiClass
fun PsiNamedElement.qualifiedClassNameForRendering(): String {
    val fqName = when (this) {
        is KtClassOrObject -> fqName?.asString()
        is PsiClass -> qualifiedName
        else -> throw AssertionError("Not a class: ${getElementTextWithContext()}")
    }
    return fqName ?: name ?: "[Anonymous]"
}

fun KotlinMemberInfo.getChildrenToAnalyze(): List<PsiElement> {
    val member = member
    val childrenToCheck = member.allChildren.toMutableList()
    if (isToAbstract && member is KtCallableDeclaration) {
        when (member) {
            is KtNamedFunction -> childrenToCheck.remove(member.bodyExpression as PsiElement?)
            is KtProperty -> {
                childrenToCheck.remove(member.initializer as PsiElement?)
                childrenToCheck.remove(member.delegateExpression as PsiElement?)
                childrenToCheck.removeAll(member.accessors)
            }
        }
    }
    return childrenToCheck
}

internal fun KtNamedDeclaration.resolveToDescriptorWrapperAware(resolutionFacade: ResolutionFacade? = null): DeclarationDescriptor {
    if (this is KtPsiClassWrapper) return psiClass.getJavaClassDescriptor(resolutionFacade ?: psiClass.javaResolutionFacade())!!
    return resolutionFacade?.resolveToDescriptor(this) ?: unsafeResolveToDescriptor()
}

internal fun PsiMember.toKtDeclarationWrapperAware(): KtNamedDeclaration? {
    if (this is PsiClass && this !is KtLightClass) return KtPsiClassWrapper(this)
    return namedUnwrappedElement as? KtNamedDeclaration
}