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

@file:JvmName("JavaResolutionUtils")

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.psi.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.structure.impl.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.scopes.JetScope

private fun PsiElement.getJavaDescriptorResolver(): JavaDescriptorResolver? {
    if (!ProjectRootsUtil.isInProjectOrLibraryClassFile(this)) return null

    @Suppress("DEPRECATION")
    return KotlinCacheService.getInstance(project).getProjectService(JvmPlatform, this.getModuleInfo(), javaClass<JavaDescriptorResolver>())
}

fun PsiMethod.getJavaMethodDescriptor(): FunctionDescriptor? {
    val method = getOriginalElement() as? PsiMethod ?: return null
    if (method.containingClass == null || !Name.isValidIdentifier(method.getName())) return null
    val resolver = method.getJavaDescriptorResolver()
    return when {
        method.isConstructor() -> resolver?.resolveConstructor(JavaConstructorImpl(method))
        else -> resolver?.resolveMethod(JavaMethodImpl(method))
    }
}

fun PsiClass.getJavaClassDescriptor(): ClassDescriptor? {
    val psiClass = originalElement as? PsiClass ?: return null
    return psiClass.getJavaDescriptorResolver()?.resolveClass(JavaClassImpl(psiClass))
}

fun PsiField.getJavaFieldDescriptor(): PropertyDescriptor? {
    val field = originalElement as? PsiField ?: return null
    return field.getJavaDescriptorResolver()?.resolveField(JavaFieldImpl(field))
}

fun PsiMember.getJavaMemberDescriptor(): DeclarationDescriptor? {
    return when (this) {
        is PsiClass -> getJavaClassDescriptor()
        is PsiMethod -> getJavaMethodDescriptor()
        is PsiField -> getJavaFieldDescriptor()
        else -> null
    }
}

public fun JavaDescriptorResolver.resolveMethod(method: JavaMethod): FunctionDescriptor? {
    return getContainingScope(method)?.getFunctions(method.name, NoLookupLocation.FROM_IDE)?.findByJavaElement(method)
}

public fun JavaDescriptorResolver.resolveConstructor(constructor: JavaConstructor): ConstructorDescriptor? {
    return resolveClass(constructor.getContainingClass())?.getConstructors()?.findByJavaElement(constructor)
}

public fun JavaDescriptorResolver.resolveField(field: JavaField): PropertyDescriptor? {
    return getContainingScope(field)?.getProperties(field.name, NoLookupLocation.FROM_IDE)?.findByJavaElement(field) as? PropertyDescriptor
}

private fun JavaDescriptorResolver.getContainingScope(member: JavaMember): JetScope? {
    val containingClass = resolveClass(member.getContainingClass())
    return if (member.isStatic())
        containingClass?.getStaticScope()
    else
        containingClass?.getDefaultType()?.getMemberScope()
}

private fun <T : DeclarationDescriptorWithSource> Collection<T>.findByJavaElement(javaElement: JavaElement): T? {
    return firstOrNull { member ->
        val memberJavaElement = (member.getOriginal().getSource() as? JavaSourceElement)?.javaElement
        when {
            memberJavaElement == javaElement ->
                true
            memberJavaElement is JavaElementImpl<*> && javaElement is JavaElementImpl<*> ->
                memberJavaElement.getPsi().isEquivalentTo(javaElement.getPsi())
            else ->
                false
        }
    }
}
