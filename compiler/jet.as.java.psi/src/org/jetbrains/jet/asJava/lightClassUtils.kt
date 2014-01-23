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

package org.jetbrains.jet.asJava

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.JetParameter
import java.util.Collections
import org.jetbrains.jet.lang.psi.JetPropertyAccessor

fun PsiElement.toLightMethods(): List<PsiMethod> =
        when (this) {
            is JetNamedFunction -> Collections.singletonList(LightClassUtil.getLightClassMethod(this))
            is JetProperty -> LightClassUtil.getLightClassPropertyMethods(this).toList()
            is JetParameter -> LightClassUtil.getLightClassPropertyMethods(this).toList()
            is JetPropertyAccessor -> Collections.singletonList(LightClassUtil.getLightClassAccessorMethod(this))
            is PsiMethod -> Collections.singletonList(this)
            else -> Collections.emptyList()
        }

fun PsiElement.getRepresentativeLightMethod(): PsiMethod? =
        when (this) {
            is JetNamedFunction -> LightClassUtil.getLightClassMethod(this)
            is JetProperty -> LightClassUtil.getLightClassPropertyMethods(this).getGetter()
            is JetParameter -> LightClassUtil.getLightClassPropertyMethods(this).getGetter()
            is JetPropertyAccessor -> LightClassUtil.getLightClassAccessorMethod(this)
            is PsiMethod -> this
            else -> null
        }