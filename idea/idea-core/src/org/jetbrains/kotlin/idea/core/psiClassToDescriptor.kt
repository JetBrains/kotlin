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

package org.jetbrains.kotlin.idea.core

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.asJava.KtLightClass
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.KtLightClassForDecompiledDeclaration
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver

public fun ResolutionFacade.psiClassToDescriptor(
        psiClass: PsiClass,
        declarationTranslator: (KtClassOrObject) -> KtClassOrObject? = { it }
): ClassifierDescriptor? {
    return if (psiClass is KtLightClass && psiClass !is KtLightClassForDecompiledDeclaration) {
        val origin = psiClass.getOrigin () ?: return null
        val declaration = declarationTranslator(origin) ?: return null
        resolveToDescriptor(declaration)
    }
    else {
        this.getFrontendService(psiClass, javaClass<JavaDescriptorResolver>()).resolveClass(JavaClassImpl(psiClass))
    }  as? ClassifierDescriptor
}