/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.highlighter.markers

import java.util.HashSet
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiClass
import com.intellij.psi.CommonClassNames
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.JetClass
import org.jetbrains.jet.asJava.LightClassUtil
import org.jetbrains.jet.lang.psi.JetNamedFunction

fun collectContainingClasses(methods: Collection<PsiMethod>): Set<PsiClass> {
    val classes = HashSet<PsiClass>()
    for (method in methods) {
        ProgressManager.checkCanceled()
        val parentClass = method.getContainingClass()
        if (parentClass != null && CommonClassNames.JAVA_LANG_OBJECT != parentClass.getQualifiedName()) {
            classes.add(parentClass)
        }
    }

    return classes
}

private fun getPsiClass(element: PsiElement?): PsiClass? {
    return when {
        element == null -> null
        element is PsiClass -> element
        element is JetClass -> LightClassUtil.getPsiClass(element)
        element.getParent() is JetClass -> LightClassUtil.getPsiClass(element.getParent() as JetClass)
        else -> null
    }
}

private fun getPsiMethod(element: PsiElement?): PsiMethod? {
    return when {
        element == null -> null
        element is PsiMethod -> element
        element.getParent() is JetNamedFunction ->
            LightClassUtil.getLightClassMethod(element.getParent() as JetNamedFunction)
        else -> null
    }
}
