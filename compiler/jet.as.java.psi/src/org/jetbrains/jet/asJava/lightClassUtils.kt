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
import com.intellij.psi.PsiParameter
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.lang.psi.JetParameterList
import org.jetbrains.jet.lang.psi.JetClass
import org.jetbrains.jet.lang.psi.JetTypeParameter
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.lang.psi.JetClassOrObject
import com.intellij.psi.PsiTypeParameter
import java.util.ArrayList
import org.jetbrains.jet.lang.psi.JetTypeParameterList
import com.intellij.psi.PsiTypeParameterListOwner
import org.jetbrains.jet.lang.resolve.java.jetAsJava.KotlinLightElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiNamedElement
import org.jetbrains.jet.lang.psi.JetCallableDeclaration
import org.jetbrains.jet.lang.psi.psiUtil.isExtensionDeclaration
import com.intellij.psi.PsiClass

public fun JetClassOrObject.toLightClass(): KotlinLightClass? = LightClassUtil.getPsiClass(this) as KotlinLightClass?

public fun JetDeclaration.toLightElements(): List<PsiNamedElement> =
        when (this) {
            is JetClassOrObject -> Collections.singletonList(LightClassUtil.getPsiClass(this))
            is JetNamedFunction -> Collections.singletonList(LightClassUtil.getLightClassMethod(this))
            is JetProperty -> LightClassUtil.getLightClassPropertyMethods(this).toList()
            is JetPropertyAccessor -> Collections.singletonList(LightClassUtil.getLightClassAccessorMethod(this))
            is JetParameter -> ArrayList<PsiNamedElement>().let { elements ->
                toPsiParameter()?.let { psiParameter -> elements.add(psiParameter) }
                LightClassUtil.getLightClassPropertyMethods(this).toCollection(elements)

                elements
            }
            is JetTypeParameter -> toPsiTypeParameters()
            else -> listOf()
        }

public fun PsiElement.toLightMethods(): List<PsiMethod> =
        when (this) {
            is JetNamedFunction -> Collections.singletonList(LightClassUtil.getLightClassMethod(this))
            is JetProperty -> LightClassUtil.getLightClassPropertyMethods(this).toList()
            is JetParameter -> LightClassUtil.getLightClassPropertyMethods(this).toList()
            is JetPropertyAccessor -> Collections.singletonList(LightClassUtil.getLightClassAccessorMethod(this))
            is PsiMethod -> Collections.singletonList(this)
            else -> listOf()
        }

public fun PsiElement.getRepresentativeLightMethod(): PsiMethod? =
        when (this) {
            is JetNamedFunction -> LightClassUtil.getLightClassMethod(this)
            is JetProperty -> LightClassUtil.getLightClassPropertyMethods(this).getGetter()
            is JetParameter -> LightClassUtil.getLightClassPropertyMethods(this).getGetter()
            is JetPropertyAccessor -> LightClassUtil.getLightClassAccessorMethod(this)
            is PsiMethod -> this
            else -> null
        }

public fun JetParameter.toPsiParameter(): PsiParameter? {
    val paramList = getParentByType(javaClass<JetParameterList>())
    if (paramList == null) return null

    val paramIndex = paramList.getParameters().indexOf(this)
    val owner = paramList.getParent()
    val lightParamIndex = if (owner != null && owner.isExtensionDeclaration()) paramIndex + 1 else paramIndex

    val method: PsiMethod? = when (owner) {
        is JetNamedFunction -> LightClassUtil.getLightClassMethod(owner)
        is JetPropertyAccessor -> LightClassUtil.getLightClassAccessorMethod(owner)
        is JetClass -> LightClassUtil.getPsiClass(owner)?.getConstructors()?.let { constructors ->
            if (constructors.isNotEmpty()) constructors[0] else null
        }
        else -> null
    }
    if (method == null) return null

    return method.getParameterList().getParameters()[lightParamIndex]
}

public fun JetTypeParameter.toPsiTypeParameters(): List<PsiTypeParameter> {
    val paramList = getParentByType(javaClass<JetTypeParameterList>())
    if (paramList == null) return listOf()

    val paramIndex = paramList.getParameters().indexOf(this)
    val jetDeclaration = paramList.getParentByType(javaClass<JetDeclaration>()) ?: return listOf()
    val lightOwners = jetDeclaration.toLightElements()

    return lightOwners.map { lightOwner -> (lightOwner as PsiTypeParameterListOwner).getTypeParameters()[paramIndex] }
}

// Returns original declaration if given PsiElement is a Kotlin light element, and element itself otherwise
public val PsiElement.unwrapped: PsiElement?
    get() = if (this is KotlinLightElement<*, *>) origin else this

public val PsiElement.namedUnwrappedElement: PsiNamedElement?
    get() = unwrapped?.getParentByType(javaClass<PsiNamedElement>())