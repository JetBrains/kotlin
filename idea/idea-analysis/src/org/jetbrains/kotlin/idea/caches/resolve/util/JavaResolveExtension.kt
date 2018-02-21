/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

@file:JvmName("JavaResolutionUtils")

package org.jetbrains.kotlin.idea.caches.resolve.util

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.lightClasses.KtLightClassForDecompiledDeclaration
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.structure.impl.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.parameterIndex
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.scopes.MemberScope

@JvmOverloads
fun PsiMethod.getJavaMethodDescriptor(resolutionFacade: ResolutionFacade = javaResolutionFacade()): FunctionDescriptor? {
    val method = originalElement as? PsiMethod ?: return null
    if (method.containingClass == null || !Name.isValidIdentifier(method.name)) return null
    val resolver = method.getJavaDescriptorResolver(resolutionFacade)
    return when {
        method.isConstructor -> resolver?.resolveConstructor(JavaConstructorImpl(method))
        else -> resolver?.resolveMethod(JavaMethodImpl(method))
    }
}

@JvmOverloads
fun PsiClass.getJavaClassDescriptor(resolutionFacade: ResolutionFacade = javaResolutionFacade()): ClassDescriptor? {
    val psiClass = originalElement as? PsiClass ?: return null
    return psiClass.getJavaDescriptorResolver(resolutionFacade)?.resolveClass(JavaClassImpl(psiClass))
}

@JvmOverloads
fun PsiField.getJavaFieldDescriptor(resolutionFacade: ResolutionFacade = javaResolutionFacade()): PropertyDescriptor? {
    val field = originalElement as? PsiField ?: return null
    return field.getJavaDescriptorResolver(resolutionFacade)?.resolveField(JavaFieldImpl(field))
}

@JvmOverloads
fun PsiMember.getJavaMemberDescriptor(resolutionFacade: ResolutionFacade = javaResolutionFacade()): DeclarationDescriptor? {
    return when (this) {
        is PsiClass -> getJavaClassDescriptor(resolutionFacade)
        is PsiMethod -> getJavaMethodDescriptor(resolutionFacade)
        is PsiField -> getJavaFieldDescriptor(resolutionFacade)
        else -> null
    }
}

@JvmOverloads
fun PsiMember.getJavaOrKotlinMemberDescriptor(resolutionFacade: ResolutionFacade = javaResolutionFacade()): DeclarationDescriptor? {
    val callable = unwrapped
    return when (callable) {
        is PsiMember -> getJavaMemberDescriptor(resolutionFacade)
        is KtDeclaration -> {
            val descriptor = resolutionFacade.resolveToDescriptor(callable)
            if (descriptor is ClassDescriptor && this is PsiMethod) descriptor.unsubstitutedPrimaryConstructor else descriptor
        }
        else -> null
    }
}

fun PsiParameter.getParameterDescriptor(resolutionFacade: ResolutionFacade = javaResolutionFacade()): ValueParameterDescriptor? {
    val method = declarationScope as? PsiMethod ?: return null
    val methodDescriptor = method.getJavaMethodDescriptor(resolutionFacade) ?: return null
    return methodDescriptor.valueParameters[parameterIndex()]
}

fun PsiClass.resolveToDescriptor(
    resolutionFacade: ResolutionFacade,
    declarationTranslator: (KtClassOrObject) -> KtClassOrObject? = { it }
): ClassDescriptor? {
    return if (this is KtLightClass && this !is KtLightClassForDecompiledDeclaration) {
        val origin = this.kotlinOrigin ?: return null
        val declaration = declarationTranslator(origin) ?: return null
        resolutionFacade.resolveToDescriptor(declaration)
    } else {
        getJavaClassDescriptor(resolutionFacade)
    } as? ClassDescriptor
}

private fun PsiElement.getJavaDescriptorResolver(resolutionFacade: ResolutionFacade): JavaDescriptorResolver? {
    return resolutionFacade.tryGetFrontendService(this, JavaDescriptorResolver::class.java)
}

private fun JavaDescriptorResolver.resolveMethod(method: JavaMethod): FunctionDescriptor? {
    return getContainingScope(method)?.getContributedFunctions(method.name, NoLookupLocation.FROM_IDE)?.findByJavaElement(method)
}

private fun JavaDescriptorResolver.resolveConstructor(constructor: JavaConstructor): ConstructorDescriptor? {
    return resolveClass(constructor.containingClass)?.constructors?.findByJavaElement(constructor)
}

private fun JavaDescriptorResolver.resolveField(field: JavaField): PropertyDescriptor? {
    return getContainingScope(field)?.getContributedVariables(field.name, NoLookupLocation.FROM_IDE)?.findByJavaElement(field)
}

private fun JavaDescriptorResolver.getContainingScope(member: JavaMember): MemberScope? {
    val containingClass = resolveClass(member.containingClass)
    return if (member.isStatic)
        containingClass?.staticScope
    else
        containingClass?.defaultType?.memberScope
}

private fun <T : DeclarationDescriptorWithSource> Collection<T>.findByJavaElement(javaElement: JavaElement): T? {
    return firstOrNull { member ->
        val memberJavaElement = (member.original.source as? JavaSourceElement)?.javaElement
        when {
            memberJavaElement == javaElement ->
                true
            memberJavaElement is JavaElementImpl<*> && javaElement is JavaElementImpl<*> ->
                memberJavaElement.psi.isEquivalentTo(javaElement.psi)
            else ->
                false
        }
    }
}

fun PsiElement.javaResolutionFacade() =
    KotlinCacheService.getInstance(project).getResolutionFacadeByFile(this.originalElement.containingFile, JvmPlatform)
