/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.elements.KtLightAnnotationForSourceEntry
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.propertyNameByGetMethodName
import org.jetbrains.kotlin.load.java.propertyNameBySetMethodName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration

fun KtClassOrObject.toLightClass(): KtLightClass? = LightClassGenerationSupport.getInstance(project).getLightClass(this)

fun KtFile.findFacadeClass(): KtLightClass? {
    return LightClassGenerationSupport.getInstance(project)
            .getFacadeClassesInPackage(packageFqName, this.useScope as? GlobalSearchScope ?: GlobalSearchScope.projectScope(project))
            .firstOrNull { it is KtLightClassForFacade && this in it.files } as? KtLightClass
}

fun KtElement.toLightElements(): List<PsiNamedElement> =
        when (this) {
            is KtClassOrObject -> listOfNotNull(toLightClass())
            is KtNamedFunction,
            is KtSecondaryConstructor -> LightClassUtil.getLightClassMethods(this as KtFunction)
            is KtProperty -> LightClassUtil.getLightClassPropertyMethods(this).allDeclarations
            is KtPropertyAccessor -> listOfNotNull(LightClassUtil.getLightClassAccessorMethod(this))
            is KtParameter -> mutableListOf<PsiNamedElement>().also { elements ->
                toPsiParameters().toCollection(elements)
                LightClassUtil.getLightClassPropertyMethods(this).toCollection(elements)
                toAnnotationLightMethod()?.let(elements::add)
            }
            is KtTypeParameter -> toPsiTypeParameters()
            is KtFile -> listOfNotNull(findFacadeClass())
            else -> listOf()
        }

fun PsiElement.toLightMethods(): List<PsiMethod> =
        when (this) {
            is KtFunction -> LightClassUtil.getLightClassMethods(this)
            is KtProperty -> LightClassUtil.getLightClassPropertyMethods(this).toList()
            is KtParameter -> LightClassUtil.getLightClassPropertyMethods(this).toList()
            is KtPropertyAccessor -> LightClassUtil.getLightClassAccessorMethods(this)
            is KtClass -> listOfNotNull(toLightClass()?.constructors?.firstOrNull())
            is PsiMethod -> listOf(this)
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

    val paramIndex = paramList.parameters.indexOf(this)
    val owner = paramList.parent
    val lightParamIndex = if (owner is KtDeclaration && owner.isExtensionDeclaration()) paramIndex + 1 else paramIndex

    val methods: Collection<PsiMethod> =
            when (owner) {
                is KtFunction -> LightClassUtil.getLightClassMethods(owner)
                is KtPropertyAccessor -> LightClassUtil.getLightClassAccessorMethods(owner)
                else -> null
            } ?: return emptyList()

    return methods.mapNotNull {
        if (it.parameterList.parametersCount > lightParamIndex) it.parameterList.parameters[lightParamIndex] else null
    }
}

private fun KtParameter.toAnnotationLightMethod(): PsiMethod? {
    val parent = ownerFunction as? KtPrimaryConstructor ?: return null
    val containingClass = parent.getContainingClassOrObject()
    if (!containingClass.isAnnotation()) return null

    return LightClassUtil.getLightClassMethod(this)
}

fun KtTypeParameter.toPsiTypeParameters(): List<PsiTypeParameter> {
    val paramList = getNonStrictParentOfType<KtTypeParameterList>() ?: return listOf()

    val paramIndex = paramList.parameters.indexOf(this)
    val ktDeclaration = paramList.getNonStrictParentOfType<KtDeclaration>() ?: return listOf()
    val lightOwners = ktDeclaration.toLightElements()

    return lightOwners.mapNotNull { lightOwner ->
        (lightOwner as? PsiTypeParameterListOwner)?.typeParameters?.getOrNull(paramIndex)
    }
}

// Returns original declaration if given PsiElement is a Kotlin light element, and element itself otherwise
val PsiElement.unwrapped: PsiElement?
    get() = when {
        this is KtLightElement<*, *> -> kotlinOrigin
        this is KtLightIdentifier -> origin
        this is KtLightAnnotationForSourceEntry.LightExpressionValue<*> -> originalExpression
        else -> this
    }

val PsiElement.namedUnwrappedElement: PsiNamedElement?
    get() = unwrapped?.getNonStrictParentOfType<PsiNamedElement>()


val KtClassOrObject.hasInterfaceDefaultImpls: Boolean
    get() = this is KtClass && isInterface() && hasNonAbstractMembers(this)

private fun hasNonAbstractMembers(ktInterface: KtClass): Boolean {
    return ktInterface.declarations.any(::isNonAbstractMember)
}

private fun isNonAbstractMember(member: KtDeclaration?): Boolean {
    return (member is KtNamedFunction && member.hasBody()) ||
           (member is KtProperty && (member.hasDelegateExpressionOrInitializer() || member.getter?.hasBody() ?: false || member.setter?.hasBody() ?: false))
}

private val DEFAULT_IMPLS_CLASS_NAME = Name.identifier(JvmAbi.DEFAULT_IMPLS_CLASS_NAME)
fun FqName.defaultImplsChild() = child(DEFAULT_IMPLS_CLASS_NAME)

@Suppress("unused")
fun KtAnnotationEntry.toLightAnnotation(): PsiAnnotation? {
    val ktDeclaration = getStrictParentOfType<KtModifierList>()?.parent as? KtDeclaration ?: return null
    for (lightElement in ktDeclaration.toLightElements()) {
        if (lightElement !is PsiModifierListOwner) continue
        lightElement.modifierList?.annotations?.firstOrNull { it is KtLightAnnotationForSourceEntry && it.kotlinOrigin == this }?.let { return it }
    }
    return null
}

fun propertyNameByAccessor(name: String, accessor: KtLightMethod): String? {
    val toRename = accessor.kotlinOrigin ?: return null
    if (toRename !is KtProperty && toRename !is KtParameter) return null

    val methodName = Name.guessByFirstCharacter(name)
    val propertyName = toRename.name ?: ""
    return when {
        JvmAbi.isGetterName(name) -> propertyNameByGetMethodName(methodName)
        JvmAbi.isSetterName(name) -> propertyNameBySetMethodName(methodName, propertyName.startsWith("is"))
        else -> methodName
    }?.asString()
}

fun accessorNameByPropertyName(name: String, accessor: KtLightMethod): String? {
    val methodName = accessor.name
    return when {
        JvmAbi.isGetterName(methodName) -> JvmAbi.getterName(name)
        JvmAbi.isSetterName(methodName) -> JvmAbi.setterName(name)
        else -> null
    }
}

fun getAccessorNamesCandidatesByPropertyName(name: String): List<String> {
    return listOf(JvmAbi.setterName(name), JvmAbi.getterName(name))
}