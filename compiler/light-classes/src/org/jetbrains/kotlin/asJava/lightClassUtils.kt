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
import com.intellij.psi.impl.light.LightField
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.asJava.elements.PsiElementWithOrigin
import org.jetbrains.kotlin.asJava.elements.*
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.propertyNameByGetMethodName
import org.jetbrains.kotlin.load.java.propertyNameBySetMethodName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.types.TypeUtils

/**
 * Can be null in scripts and for elements from non-jvm modules.
 */
fun KtClassOrObject.toLightClass(): KtLightClass? = KotlinAsJavaSupport.getInstance(project).getLightClass(this)

fun KtClassOrObject.toLightClassWithBuiltinMapping(): PsiClass? {
    toLightClass()?.let { return it }

    val fqName = fqName ?: return null
    val javaClassFqName = JavaToKotlinClassMap.mapKotlinToJava(fqName.toUnsafe())?.asSingleFqName() ?: return null
    val searchScope = useScope as? GlobalSearchScope ?: return null
    return JavaPsiFacade.getInstance(project).findClass(javaClassFqName.asString(), searchScope)
}

fun KtClassOrObject.toFakeLightClass(): KtFakeLightClass = KotlinAsJavaSupport.getInstance(project).getFakeLightClass(this)

fun KtFile.findFacadeClass(): KtLightClass? {
    return KotlinAsJavaSupport.getInstance(project)
            .getFacadeClassesInPackage(packageFqName, this.useScope as? GlobalSearchScope ?: GlobalSearchScope.projectScope(project))
            .firstOrNull { it is KtLightClassForFacade && this in it.files } as? KtLightClass
}

fun KtScript.toLightClass(): KtLightClass? = KotlinAsJavaSupport.getInstance(project).getLightClassForScript(this)

fun KtElement.toLightElements(): List<PsiNamedElement> =
        when (this) {
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
    if (paramIndex < 0) return emptyList()
    val owner = paramList.parent
    val lightParamIndex = if (owner is KtDeclaration && owner.isExtensionDeclaration()) paramIndex + 1 else paramIndex

    val methods: Collection<PsiMethod> =
            when (owner) {
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

// Returns original declaration if given PsiElement is a Kotlin light element, and element itself otherwise
val PsiElement.unwrapped: PsiElement?
    get() = when {
        this is PsiElementWithOrigin<*> -> origin
        this is KtLightElement<*, *> -> kotlinOrigin
        this is KtLightElementBase -> kotlinOrigin
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
fun KtElement.toLightAnnotation(): PsiAnnotation? {
    val ktDeclaration = getStrictParentOfType<KtModifierList>()?.parent as? KtDeclaration ?: return null
    for (lightElement in ktDeclaration.toLightElements()) {
        if (lightElement !is PsiModifierListOwner) continue
        for (rootAnnotation in lightElement.modifierList?.annotations ?: continue) {
            for (annotation in rootAnnotation.withNestedAnnotations()) {
                if (annotation is KtLightAnnotationForSourceEntry && annotation.kotlinOrigin == this)
                    return annotation
            }
        }
    }
    return null
}

private fun PsiAnnotation.withNestedAnnotations(): Sequence<PsiAnnotation> {
    fun handleValue(memberValue: PsiAnnotationMemberValue?): Sequence<PsiAnnotation> =
        when (memberValue) {
            is PsiArrayInitializerMemberValue ->
                memberValue.initializers.asSequence().flatMap { handleValue(it) }
            is PsiAnnotation -> memberValue.withNestedAnnotations()
            else -> emptySequence()
        }
    return sequenceOf(this) + parameterList.attributes.asSequence().flatMap { handleValue(it.value) }
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

fun KtLightMethod.checkIsMangled(): Boolean {
    val demangledName = KotlinTypeMapper.InternalNameMapper.demangleInternalName(name) ?: return false
    val originalName = propertyNameByAccessor(demangledName, this) ?: demangledName
    return originalName == kotlinOrigin?.name
}

fun fastCheckIsNullabilityApplied(lightElement: KtLightElement<*, PsiModifierListOwner>): Boolean {

    val elementIsApplicable =
        (lightElement is KtLightMember<*> && lightElement !is KtLightFieldImpl.KtLightEnumConstant) || lightElement is LightParameter
    if (!elementIsApplicable) return false

    val annotatedElement = lightElement.kotlinOrigin ?: return true

    // all data-class generated members are not-null
    if (annotatedElement is KtClass && annotatedElement.isData()) return true

    // backing fields for lateinit props are skipped
    if (lightElement is KtLightField && annotatedElement is KtProperty && annotatedElement.hasModifier(KtTokens.LATEINIT_KEYWORD)) return false

    if (lightElement is KtLightMethod && (annotatedElement as? KtModifierListOwner)?.isPrivate() == true) {
        return false
    }

    if (annotatedElement is KtParameter) {
        val containingClassOrObject = annotatedElement.containingClassOrObject
        if (containingClassOrObject?.isAnnotation() == true) return false
        if ((containingClassOrObject as? KtClass)?.isEnum() == true) {
            if (annotatedElement.parent.parent is KtPrimaryConstructor) return false
        }

        when (val parent = annotatedElement.parent.parent) {
            is KtConstructor<*> -> {
                if (lightElement is KtLightParameter && parent.isPrivate()) return false
            }
            is KtNamedFunction -> {
                return !parent.isPrivate()
            }
            is KtPropertyAccessor -> {
                return (parent.parent as? KtProperty)?.isPrivate() != true
            }
        }
    }

    return true
}

fun computeExpression(expression: PsiElement): Any? {
    fun evalConstantValue(constantValue: ConstantValue<*>): Any? {
        return if (constantValue is ArrayValue) {
            val items = constantValue.value.map { evalConstantValue(it) }
            items.singleOrNull() ?: items
        } else constantValue.value
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