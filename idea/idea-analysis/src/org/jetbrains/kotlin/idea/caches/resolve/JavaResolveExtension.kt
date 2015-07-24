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

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.structure.impl.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.JvmResolverForModule
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.scopes.JetScope

public object JavaResolveExtension : CacheExtension<(PsiElement) -> Pair<JavaDescriptorResolver, BindingContext>> {
    override val platform: TargetPlatform = JvmPlatform

    override fun getData(resolverProvider: ModuleResolverProvider): (PsiElement) -> Pair<JavaDescriptorResolver, BindingContext> {
        return {
            val resolverForModule = resolverProvider.resolverByModule(it.getModuleInfo()) as JvmResolverForModule
            Pair(resolverForModule.javaDescriptorResolver, resolverForModule.lazyResolveSession.getBindingContext())
        }
    }

    public fun getResolver(project: Project, element: PsiElement): JavaDescriptorResolver =
            KotlinCacheService.getInstance(project)[this](element).first

    public fun getContext(project: Project, element: PsiElement): BindingContext =
            KotlinCacheService.getInstance(project)[this](element).second
}

fun PsiMethod.getJavaMethodDescriptor(): FunctionDescriptor? {
    val method = getOriginalElement() as? PsiMethod ?: return null
    val resolver = JavaResolveExtension.getResolver(method.getProject(), method)
    return when {
        method.isConstructor() -> resolver.resolveConstructor(JavaConstructorImpl(method))
        else -> resolver.resolveMethod(JavaMethodImpl(method))
    }
}

fun PsiClass.getJavaClassDescriptor(): ClassDescriptor? {
    return JavaResolveExtension.getResolver(getProject(), this).resolveClass(JavaClassImpl(this))
}

fun PsiField.getJavaFieldDescriptor(): PropertyDescriptor? {
    return JavaResolveExtension.getResolver(getProject(), this).resolveField(JavaFieldImpl(this))
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
    return getContainingScope(method)?.getFunctions(method.getName())?.findByJavaElement(method)
}

public fun JavaDescriptorResolver.resolveConstructor(constructor: JavaConstructor): ConstructorDescriptor? {
    return resolveClass(constructor.getContainingClass())?.getConstructors()?.findByJavaElement(constructor)
}

public fun JavaDescriptorResolver.resolveField(field: JavaField): PropertyDescriptor? {
    return getContainingScope(field)?.getProperties(field.getName())?.findByJavaElement(field) as? PropertyDescriptor
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
