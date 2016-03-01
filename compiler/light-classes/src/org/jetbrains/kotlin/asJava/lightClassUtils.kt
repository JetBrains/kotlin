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
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.utils.addToStdlib.singletonList
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import java.util.*

fun KtClassOrObject.toLightClass(): KtLightClass? = LightClassGenerationSupport.getInstance(project).getLightClass(this)

fun KtFile.findFacadeClass(): KtLightClass? {
    return LightClassGenerationSupport.getInstance(project)
            .getFacadeClassesInPackage(packageFqName, this.useScope as? GlobalSearchScope ?: GlobalSearchScope.projectScope(project))
            .firstOrNull { it is KtLightClassForFacade && this in it.files } as? KtLightClass
}

fun KtDeclaration.toLightElements(): List<PsiNamedElement> =
        when (this) {
            is KtClassOrObject -> toLightClass().singletonOrEmptyList()
            is KtNamedFunction,
            is KtSecondaryConstructor -> LightClassUtil.getLightClassMethods(this as KtFunction)
            is KtProperty -> LightClassUtil.getLightClassPropertyMethods(this).allDeclarations
            is KtPropertyAccessor -> LightClassUtil.getLightClassAccessorMethod(this).singletonOrEmptyList()
            is KtParameter -> ArrayList<PsiNamedElement>().let { elements ->
                toPsiParameters().toCollection(elements)
                LightClassUtil.getLightClassPropertyMethods(this).toCollection(elements)

                elements
            }
            is KtTypeParameter -> toPsiTypeParameters()
            else -> listOf()
        }

fun PsiElement.toLightMethods(): List<PsiMethod> =
        when (this) {
            is KtFunction -> LightClassUtil.getLightClassMethods(this)
            is KtProperty -> LightClassUtil.getLightClassPropertyMethods(this).toList()
            is KtParameter -> LightClassUtil.getLightClassPropertyMethods(this).toList()
            is KtPropertyAccessor -> LightClassUtil.getLightClassAccessorMethods(this)
            is KtClass -> toLightClass()?.getConstructors()?.first().singletonOrEmptyList()
            is PsiMethod -> this.singletonList()
            else -> listOf()
        }

fun PsiElement.getRepresentativeLightMethod(): PsiMethod? =
        when (this) {
            is KtFunction -> LightClassUtil.getLightClassMethod(this)
            is KtProperty -> LightClassUtil.getLightClassPropertyMethods(this).getter
            is KtParameter -> LightClassUtil.getLightClassPropertyMethods(this).getter
            is KtPropertyAccessor -> LightClassUtil.getLightClassAccessorMethod(this)
            is PsiMethod -> this
            else -> null
        }

fun KtParameter.toPsiParameters(): Collection<PsiParameter> {
    val paramList = getNonStrictParentOfType<KtParameterList>() ?: return emptyList()

    val paramIndex = paramList.getParameters().indexOf(this)
    val owner = paramList.getParent()
    val lightParamIndex = if (owner is KtDeclaration && owner.isExtensionDeclaration()) paramIndex + 1 else paramIndex

    val methods: Collection<PsiMethod> =
            when (owner) {
                is KtFunction -> LightClassUtil.getLightClassMethods(owner)
                is KtPropertyAccessor -> LightClassUtil.getLightClassAccessorMethods(owner)
                else -> null
            } ?: return emptyList()

    return methods.map { it.parameterList.parameters[lightParamIndex] }
}

fun KtTypeParameter.toPsiTypeParameters(): List<PsiTypeParameter> {
    val paramList = getNonStrictParentOfType<KtTypeParameterList>()
    if (paramList == null) return listOf()

    val paramIndex = paramList.getParameters().indexOf(this)
    val jetDeclaration = paramList.getNonStrictParentOfType<KtDeclaration>() ?: return listOf()
    val lightOwners = jetDeclaration.toLightElements()

    return lightOwners.map { lightOwner -> (lightOwner as PsiTypeParameterListOwner).typeParameters[paramIndex] }
}

// Returns original declaration if given PsiElement is a Kotlin light element, and element itself otherwise
val PsiElement.unwrapped: PsiElement?
    get() = if (this is KtLightElement<*, *>) getOrigin() else this

val PsiElement.namedUnwrappedElement: PsiNamedElement?
    get() = unwrapped?.getNonStrictParentOfType<PsiNamedElement>()


val KtClassOrObject.hasInterfaceDefaultImpls: Boolean
    get() = this is KtClass && isInterface() && hasNonAbstractMembers(this)

private fun hasNonAbstractMembers(ktInterface: KtClass): Boolean {
    return ktInterface.declarations.any {
        isNonAbstractMember(it)
    }
}

private fun isNonAbstractMember(member: KtDeclaration?): Boolean {
    return (member is KtNamedFunction && member.hasBody()) ||
           (member is KtProperty && (member.hasDelegateExpressionOrInitializer() || member.getter?.hasBody() ?: false || member.setter?.hasBody() ?: false))
}

private val DEFAULT_IMPLS_CLASS_NAME = Name.identifier(JvmAbi.DEFAULT_IMPLS_CLASS_NAME)
fun FqName.defaultImplsChild() = child(DEFAULT_IMPLS_CLASS_NAME)

fun KtAnnotationEntry.toLightAnnotation(): PsiAnnotation? {
    val ktDeclaration = getStrictParentOfType<KtModifierList>()?.parent as? KtDeclaration ?: return null
    val lightElement = ktDeclaration.toLightElements().firstOrNull() as? PsiModifierListOwner ?: return null
    return lightElement.modifierList?.annotations?.firstOrNull { it is KtLightAnnotation && it.getOrigin() == this }
}