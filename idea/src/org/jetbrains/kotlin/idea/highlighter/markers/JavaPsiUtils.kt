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

package org.jetbrains.kotlin.idea.highlighter.markers

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.asJava.*
import org.jetbrains.kotlin.idea.caches.resolve.lightClasses.KtFakeLightClass
import org.jetbrains.kotlin.idea.caches.resolve.lightClasses.KtFakeLightMethod
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import java.util.*

fun collectContainingClasses(methods: Collection<PsiMethod>): Set<PsiClass> {
    val classes = HashSet<PsiClass>()
    for (method in methods) {
        ProgressManager.checkCanceled()
        val parentClass = method.containingClass
        if (parentClass != null && CommonClassNames.JAVA_LANG_OBJECT != parentClass.qualifiedName) {
            classes.add(parentClass)
        }
    }

    return classes
}

internal tailrec fun getPsiClass(element: PsiElement?): PsiClass? {
    return when {
        element == null -> null
        element is PsiClass -> element
        element is KtClass -> element.toLightClass() ?: KtFakeLightClass(element)
        element.parent is KtClass -> getPsiClass(element.parent)
        else -> null
    }
}

internal fun getPsiMethod(element: PsiElement?): PsiMethod? {
    val parent = element?.parent
    return when {
        element == null -> null
        element is PsiMethod -> element
        parent is KtNamedFunction || parent is KtSecondaryConstructor ->
            LightClassUtil.getLightClassMethod(parent as KtFunction) ?: KtFakeLightMethod.get(parent)
        else -> null
    }
}
