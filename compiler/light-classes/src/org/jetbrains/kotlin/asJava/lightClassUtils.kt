/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.classes.runReadAction
import org.jetbrains.kotlin.asJava.elements.*
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.types.TypeUtils

fun KtElement.toLightElements(): List<PsiNamedElement> = when (this) {
    is KtClassOrObject -> listOfNotNull(toLightClass())
    is KtNamedFunction,
    is KtConstructor<*> -> LightClassUtil.getLightClassMethods(this as KtFunction)
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

fun PsiElement.toLightMethods(): List<PsiMethod> = when (this) {
    is KtFunction -> LightClassUtil.getLightClassMethods(this)
    is KtProperty -> LightClassUtil.getLightClassPropertyMethods(this).toList()
    is KtParameter -> LightClassUtil.getLightClassPropertyMethods(this).toList()
    is KtPropertyAccessor -> LightClassUtil.getLightClassAccessorMethods(this)
    is KtClass -> listOfNotNull(toLightClass()?.constructors?.firstOrNull())
    is PsiMethod -> listOf(this)
    else -> listOf()
}

fun PsiElement.getRepresentativeLightMethod(): PsiMethod? = when (this) {
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
    if (paramIndex < 0) return emptyList()
    val owner = paramList.parent
    val lightParamIndex = if (owner is KtDeclaration && owner.isExtensionDeclaration()) paramIndex + 1 else paramIndex

    val methods: Collection<PsiMethod> = when (owner) {
        is KtFunction -> LightClassUtil.getLightClassMethods(owner)
        is KtPropertyAccessor -> LightClassUtil.getLightClassAccessorMethods(owner)
        else -> null
    } ?: return emptyList()

    return methods.mapNotNull { it.parameterList.parameters.getOrNull(lightParamIndex) }
}

private fun KtParameter.toAnnotationLightMethod(): PsiMethod? {
    val parent = ownerFunction as? KtPrimaryConstructor ?: return null
    val containingClass = parent.getContainingClassOrObject()
    if (!containingClass.isAnnotation()) return null

    return LightClassUtil.getLightClassMethod(this)
}

fun KtParameter.toLightGetter(): PsiMethod? = LightClassUtil.getLightClassPropertyMethods(this).getter

fun KtParameter.toLightSetter(): PsiMethod? = LightClassUtil.getLightClassPropertyMethods(this).setter

fun KtTypeParameter.toPsiTypeParameters(): List<PsiTypeParameter> {
    val paramList = getNonStrictParentOfType<KtTypeParameterList>() ?: return listOf()

    val paramIndex = paramList.parameters.indexOf(this)
    val ktDeclaration = paramList.getNonStrictParentOfType<KtDeclaration>() ?: return listOf()
    val lightOwners = ktDeclaration.toLightElements()

    return lightOwners.mapNotNull { lightOwner ->
        (lightOwner as? PsiTypeParameterListOwner)?.typeParameters?.getOrNull(paramIndex)
    }
}

@Suppress("unused")
fun KtElement.toLightAnnotation(): PsiAnnotation? {
    val ktDeclaration = getStrictParentOfType<KtModifierList>()?.parent as? KtDeclaration ?: return null
    for (lightElement in ktDeclaration.toLightElements()) {
        if (lightElement !is PsiModifierListOwner) continue
        for (rootAnnotation in lightElement.modifierList?.annotations ?: continue) {
            for (annotation in rootAnnotation.withNestedAnnotations()) {
                if (annotation is KtLightElement<*, *> && annotation.kotlinOrigin == this)
                    return annotation
            }
        }
    }
    return null
}

private fun PsiAnnotation.withNestedAnnotations(): Sequence<PsiAnnotation> {
    fun handleValue(memberValue: PsiAnnotationMemberValue?): Sequence<PsiAnnotation> = when (memberValue) {
        is PsiArrayInitializerMemberValue -> memberValue.initializers.asSequence().flatMap { handleValue(it) }
        is PsiAnnotation -> memberValue.withNestedAnnotations()
        else -> emptySequence()
    }

    return sequenceOf(this) + parameterList.attributes.asSequence().flatMap { handleValue(it.value) }
}

fun KtLightMethod.checkIsMangled(): Boolean {
    val demangledName = KotlinTypeMapper.InternalNameMapper.demangleInternalName(name) ?: return false
    val originalName = propertyNameByAccessor(demangledName, this) ?: demangledName
    return originalName == kotlinOrigin?.name
}

fun computeExpression(expression: PsiElement): Any? {
    fun evalConstantValue(constantValue: ConstantValue<*>): Any? =
        if (constantValue is ArrayValue) {
            val items = constantValue.value.map { evalConstantValue(it) }
            items.singleOrNull() ?: items
        } else {
            constantValue.value
        }

    val expressionToCompute = when (expression) {
        is KtLightElementBase -> expression.kotlinOrigin as? KtExpression ?: return null
        else -> return null
    }

    val generationSupport = LightClassGenerationSupport.getInstance(expressionToCompute.project)
    val evaluator = generationSupport.createConstantEvaluator(expressionToCompute)

    val constant = runReadAction {
        val evaluatorTrace = DelegatingBindingTrace(generationSupport.analyze(expressionToCompute), "Evaluating annotation argument")
        evaluator.evaluateExpression(expressionToCompute, evaluatorTrace)
    } ?: return null

    if (constant.isError) return null
    return evalConstantValue(constant.toConstantValue(TypeUtils.NO_EXPECTED_TYPE))
}
