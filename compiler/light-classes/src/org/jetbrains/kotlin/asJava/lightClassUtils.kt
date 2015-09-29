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

package org.jetbrains.kotlin.asJava

import com.intellij.psi.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.utils.addToStdlib.singletonList
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import java.util.*

public fun JetClassOrObject.toLightClass(): KotlinLightClass? = LightClassUtil.getPsiClass(this) as KotlinLightClass?

public fun JetDeclaration.toLightElements(): List<PsiNamedElement> =
        when (this) {
            is JetClassOrObject -> LightClassUtil.getPsiClass(this).singletonOrEmptyList()
            is JetNamedFunction,
            is JetSecondaryConstructor -> LightClassUtil.getLightClassMethods(this as JetFunction)
            is JetProperty -> LightClassUtil.getLightClassPropertyMethods(this).toList()
            is JetPropertyAccessor -> LightClassUtil.getLightClassAccessorMethod(this).singletonOrEmptyList()
            is JetParameter -> ArrayList<PsiNamedElement>().let { elements ->
                toPsiParameters().toCollection(elements)
                LightClassUtil.getLightClassPropertyMethods(this).toCollection(elements)

                elements
            }
            is JetTypeParameter -> toPsiTypeParameters()
            else -> listOf()
        }

public fun PsiElement.toLightMethods(): List<PsiMethod> =
        when (this) {
            is JetFunction -> LightClassUtil.getLightClassMethods(this)
            is JetProperty -> LightClassUtil.getLightClassPropertyMethods(this).toList()
            is JetParameter -> LightClassUtil.getLightClassPropertyMethods(this).toList()
            is JetPropertyAccessor -> LightClassUtil.getLightClassAccessorMethods(this)
            is JetClass -> LightClassUtil.getPsiClass(this)?.getConstructors()?.first().singletonOrEmptyList()
            is PsiMethod -> this.singletonList()
            else -> listOf()
        }

public fun PsiElement.getRepresentativeLightMethod(): PsiMethod? =
        when (this) {
            is JetFunction -> LightClassUtil.getLightClassMethod(this)
            is JetProperty -> LightClassUtil.getLightClassPropertyMethods(this).getter
            is JetParameter -> LightClassUtil.getLightClassPropertyMethods(this).getter
            is JetPropertyAccessor -> LightClassUtil.getLightClassAccessorMethod(this)
            is PsiMethod -> this
            else -> null
        }

public fun JetParameter.toPsiParameters(): Collection<PsiParameter> {
    val paramList = getNonStrictParentOfType<JetParameterList>() ?: return emptyList()

    val paramIndex = paramList.getParameters().indexOf(this)
    val owner = paramList.getParent()
    val lightParamIndex = if (owner is JetDeclaration && owner.isExtensionDeclaration()) paramIndex + 1 else paramIndex

    val methods: Collection<PsiMethod> =
            when (owner) {
                is JetFunction -> LightClassUtil.getLightClassMethods(owner)
                is JetPropertyAccessor -> LightClassUtil.getLightClassAccessorMethods(owner)
                else -> null
            } ?: return emptyList()

    return methods.map { it.getParameterList().getParameters()[lightParamIndex] }
}

public fun JetTypeParameter.toPsiTypeParameters(): List<PsiTypeParameter> {
    val paramList = getNonStrictParentOfType<JetTypeParameterList>()
    if (paramList == null) return listOf()

    val paramIndex = paramList.getParameters().indexOf(this)
    val jetDeclaration = paramList.getNonStrictParentOfType<JetDeclaration>() ?: return listOf()
    val lightOwners = jetDeclaration.toLightElements()

    return lightOwners.map { lightOwner -> (lightOwner as PsiTypeParameterListOwner).getTypeParameters()[paramIndex] }
}

// Returns original declaration if given PsiElement is a Kotlin light element, and element itself otherwise
public val PsiElement.unwrapped: PsiElement?
    get() = if (this is KotlinLightElement<*, *>) getOrigin() else this

public val PsiElement.namedUnwrappedElement: PsiNamedElement?
    get() = unwrapped?.getNonStrictParentOfType<PsiNamedElement>()


val JetClassOrObject.hasInterfaceDefaultImpls: Boolean
    get() = this is JetClass && isInterface()

private val DEFAULT_IMPLS_CLASS_NAME = Name.identifier(JvmAbi.DEFAULT_IMPLS_CLASS_NAME)
fun FqName.defaultImplsChild() = child(DEFAULT_IMPLS_CLASS_NAME)
