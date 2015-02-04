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

import org.jetbrains.kotlin.idea.project.TargetPlatform
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.resolve.jvm.JvmResolverForModule
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.BindingContext
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.asJava.KotlinLightMethod
import org.jetbrains.kotlin.resolve.jvm.resolveMethod
import org.jetbrains.kotlin.load.java.structure.impl.JavaMethodImpl
import org.jetbrains.kotlin.load.java.structure.impl.JavaConstructorImpl
import org.jetbrains.kotlin.resolve.jvm.resolveConstructor

public object JavaResolveExtension : CacheExtension<(PsiElement) -> Pair<JavaDescriptorResolver, BindingContext>> {
    override val platform: TargetPlatform = TargetPlatform.JVM

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

fun PsiMethod.getJavaMethodDescriptor(): FunctionDescriptor {
    if (this is KotlinLightMethod) throw AssertionError("Light methods are not allowed here: ${getOrigin()?.getText()}")

    val resolver = JavaResolveExtension.getResolver(getProject(), this)
    val methodDescriptor = when {
        this.isConstructor() -> resolver.resolveConstructor(JavaConstructorImpl(this))
        else -> resolver.resolveMethod(JavaMethodImpl(this))
    }
    assert(methodDescriptor != null) { "No descriptor found for " + getText() }

    return methodDescriptor!!
}
