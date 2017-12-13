/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix.crossLanguage

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.QuickFixFactory
import com.intellij.lang.jvm.*
import com.intellij.lang.jvm.actions.*
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.MutablePackageFragmentDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.appendModifier
import org.jetbrains.kotlin.idea.quickfix.AddModifierFix
import org.jetbrains.kotlin.idea.quickfix.RemoveModifierFix
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable.CreateCallableFromUsageFix
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.approximateFlexibleTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.components.TypeUsage
import org.jetbrains.kotlin.load.java.lazy.JavaResolverComponents
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.TypeParameterResolver
import org.jetbrains.kotlin.load.java.lazy.child
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaTypeParameterDescriptor
import org.jetbrains.kotlin.load.java.lazy.types.JavaTypeAttributes
import org.jetbrains.kotlin.load.java.lazy.types.JavaTypeResolver
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.load.java.structure.impl.JavaTypeImpl
import org.jetbrains.kotlin.load.java.structure.impl.JavaTypeParameterImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.annotations.JVM_FIELD_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.supertypes

class KotlinElementActionsFactory : JvmElementActionsFactory() {
    companion object {
        val javaPsiModifiersMapping = mapOf(
                JvmModifier.PRIVATE to KtTokens.PRIVATE_KEYWORD,
                JvmModifier.PUBLIC to KtTokens.PUBLIC_KEYWORD,
                JvmModifier.PROTECTED to KtTokens.PUBLIC_KEYWORD,
                JvmModifier.ABSTRACT to KtTokens.ABSTRACT_KEYWORD
        )
    }

    private class FakeExpressionFromParameter(private val psiParam: PsiParameter) : PsiReferenceExpressionImpl() {
        override fun getText(): String = psiParam.name!!
        override fun getProject(): Project = psiParam.project
        override fun getParent(): PsiElement = psiParam.parent
        override fun getType(): PsiType? = psiParam.type
        override fun isValid(): Boolean = true
        override fun getContainingFile(): PsiFile = psiParam.containingFile
        override fun getReferenceName(): String? = psiParam.name
        override fun resolve(): PsiElement? = psiParam
    }

    private class ModifierBuilder(
            private val targetContainer: KtElement,
            private val allowJvmStatic: Boolean = true
    ) {
        private val psiFactory = KtPsiFactory(targetContainer.project)

        val modifierList = psiFactory.createEmptyModifierList()

        private fun JvmModifier.transformAndAppend(): Boolean {
            javaPsiModifiersMapping[this]?.let {
                modifierList.appendModifier(it)
                return true
            }

            when (this) {
                JvmModifier.STATIC -> {
                    if (allowJvmStatic && targetContainer is KtClassOrObject) {
                        addAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME)
                    }
                }
                JvmModifier.ABSTRACT -> modifierList.appendModifier(KtTokens.ABSTRACT_KEYWORD)
                JvmModifier.FINAL -> modifierList.appendModifier(KtTokens.FINAL_KEYWORD)
                else -> return false
            }

            return true
        }

        var isValid = true
            private set

        fun addJvmModifier(modifier: JvmModifier) {
            isValid = isValid && modifier.transformAndAppend()
        }

        fun addJvmModifiers(modifiers: Iterable<JvmModifier>) {
            modifiers.forEach { addJvmModifier(it) }
        }

        fun addAnnotation(fqName: FqName) {
            if (!isValid) return
            modifierList.add(psiFactory.createAnnotationEntry("@${fqName.asString()}"))
        }
    }

    class CreatePropertyFix(
            private val targetClass: JvmClass,
            contextElement: KtElement,
            propertyInfo: PropertyInfo
    ) : CreateCallableFromUsageFix<KtElement>(contextElement, listOf(propertyInfo)) {
        override fun getFamilyName() = "Add property"
        override fun getText(): String {
            val info = callableInfos.first() as PropertyInfo
            return buildString {
                append("Add '")
                if (info.isLateinitPreferred) {
                    append("lateinit ")
                }
                append(if (info.writable) "var" else "val")
                append("' property '${info.name}' to '${targetClass.name}'")
            }
        }
    }

    private fun JvmClass.toKtClassOrFile(): KtElement? {
        val psi = sourceElement
        return when (psi) {
            is KtClassOrObject -> psi
            is KtLightClassForSourceDeclaration -> psi.kotlinOrigin
            is KtLightClassForFacade -> psi.files.firstOrNull()
            else -> null
        }
    }

    private inline fun <reified T : KtElement> JvmElement.toKtElement() = sourceElement?.unwrapped as? T

    private fun fakeParametersExpressions(parameters: List<JvmParameter>, project: Project): Array<PsiExpression>? =
            when {
                parameters.isEmpty() -> emptyArray()
                else -> JavaPsiFacade
                        .getElementFactory(project)
                        .createParameterList(
                                parameters.map { it.name }.toTypedArray(),
                                parameters.map { it.type as? PsiType ?: return null }.toTypedArray()
                        )
                        .parameters
                        .map(::FakeExpressionFromParameter)
                        .toTypedArray()
            }

    private fun PsiType.collectTypeParameters(): List<PsiTypeParameter> {
        val results = ArrayList<PsiTypeParameter>()
        accept(
                object : PsiTypeVisitor<Unit>() {
                    override fun visitArrayType(arrayType: PsiArrayType) {
                        arrayType.componentType.accept(this)
                    }

                    override fun visitClassType(classType: PsiClassType) {
                        (classType.resolve() as? PsiTypeParameter)?.let { results += it }
                        classType.parameters.forEach { it.accept(this) }
                    }

                    override fun visitWildcardType(wildcardType: PsiWildcardType) {
                        wildcardType.bound?.accept(this)
                    }
                }
        )
        return results
    }

    private fun PsiType.resolveToKotlinType(resolutionFacade: ResolutionFacade): KotlinType? {
        val typeParameters = collectTypeParameters()
        val components = resolutionFacade.getFrontendService(JavaResolverComponents::class.java)
        val rootContext = LazyJavaResolverContext(components, TypeParameterResolver.EMPTY) { null }
        val dummyPackageDescriptor = MutablePackageFragmentDescriptor(resolutionFacade.moduleDescriptor, FqName("dummy"))
        val dummyClassDescriptor = ClassDescriptorImpl(
                dummyPackageDescriptor,
                Name.identifier("Dummy"),
                Modality.FINAL,
                ClassKind.CLASS,
                emptyList(),
                SourceElement.NO_SOURCE,
                false
        )
        val typeParameterResolver = object : TypeParameterResolver {
            override fun resolveTypeParameter(javaTypeParameter: JavaTypeParameter): TypeParameterDescriptor? {
                val psiTypeParameter = (javaTypeParameter as JavaTypeParameterImpl).psi
                val index = typeParameters.indexOf(psiTypeParameter)
                if (index < 0) return null
                return LazyJavaTypeParameterDescriptor(rootContext.child(this), javaTypeParameter, index, dummyClassDescriptor)
            }
        }
        val typeResolver = JavaTypeResolver(rootContext, typeParameterResolver)
        val attributes = JavaTypeAttributes(TypeUsage.COMMON)
        return typeResolver.transformJavaType(JavaTypeImpl.create(this), attributes).approximateFlexibleTypes(preferNotNull = true)
    }

    private fun ExpectedTypes.toKotlinTypeInfo(resolutionFacade: ResolutionFacade): TypeInfo {
        val candidateTypes = flatMapTo(LinkedHashSet<KotlinType>()) {
            val ktType = (it.theType as? PsiType)?.resolveToKotlinType(resolutionFacade) ?: return@flatMapTo emptyList()
            when (it.theKind) {
                ExpectedType.Kind.EXACT, ExpectedType.Kind.SUBTYPE -> listOf(ktType)
                ExpectedType.Kind.SUPERTYPE -> listOf(ktType) + ktType.supertypes()
            }
        }
        if (candidateTypes.isEmpty()) {
            val nullableAnyType = resolutionFacade.moduleDescriptor.builtIns.nullableAnyType
            return TypeInfo(nullableAnyType, Variance.INVARIANT)
        }
        return TypeInfo.ByExplicitCandidateTypes(candidateTypes.toList())
    }

    override fun createChangeModifierActions(target: JvmModifiersOwner, request: MemberRequest.Modifier): List<IntentionAction> {
        val kModifierOwner = target.toKtElement<KtModifierListOwner>() ?: return emptyList()

        val modifier = request.modifier
        val shouldPresent = request.shouldPresent
        val (kToken, shouldPresentMapped) = if (JvmModifier.FINAL == modifier)
            KtTokens.OPEN_KEYWORD to !shouldPresent
        else
            javaPsiModifiersMapping[modifier] to shouldPresent
        if (kToken == null) return emptyList()

        val action = if (shouldPresentMapped)
            AddModifierFix.createIfApplicable(kModifierOwner, kToken)
        else
            RemoveModifierFix(kModifierOwner, kToken, false)
        return listOfNotNull(action)
    }

    override fun createAddConstructorActions(targetClass: JvmClass, request: MemberRequest.Constructor): List<IntentionAction> {
        val targetKtClass = targetClass.toKtClassOrFile() as? KtClass ?: return emptyList()

        if (request.typeParameters.isNotEmpty()) return emptyList()

        val modifierBuilder = ModifierBuilder(targetKtClass).apply { addJvmModifiers(request.modifiers) }
        if (!modifierBuilder.isValid) return emptyList()

        val resolutionFacade = targetKtClass.getResolutionFacade()
        val nullableAnyType = resolutionFacade.moduleDescriptor.builtIns.nullableAnyType
        val parameterInfos = request.parameters.mapIndexed { index, param ->
            val ktType = (param.type as? PsiType)?.resolveToKotlinType(resolutionFacade) ?: nullableAnyType
            val name = param.name ?: "arg${index + 1}"
            ParameterInfo(TypeInfo(ktType, Variance.IN_VARIANCE), listOf(name))
        }
        val needPrimary = !targetKtClass.hasExplicitPrimaryConstructor()
        val constructorInfo = ConstructorInfo(
                parameterInfos,
                targetKtClass,
                isPrimary = needPrimary,
                modifierList = modifierBuilder.modifierList,
                withBody = true
        )
        val addConstructorAction = object : CreateCallableFromUsageFix<KtElement>(targetKtClass, listOf(constructorInfo)) {
            override fun getFamilyName() = "Add method"
            override fun getText() = "Add ${if (needPrimary) "primary" else "secondary"} constructor to '${targetClass.name}'"
        }

        val changePrimaryConstructorAction = run {
            val primaryConstructor = targetKtClass.primaryConstructor ?: return@run null
            val lightMethod = primaryConstructor.toLightMethods().firstOrNull() ?: return@run null
            val project = targetKtClass.project
            val fakeParametersExpressions = fakeParametersExpressions(request.parameters, project) ?: return@run null
            QuickFixFactory.getInstance()
                    .createChangeMethodSignatureFromUsageFix(
                            lightMethod,
                            fakeParametersExpressions,
                            PsiSubstitutor.EMPTY,
                            targetKtClass,
                            false,
                            2
                    ).takeIf { it.isAvailable(project, null, targetKtClass.containingFile) }
        }

        return listOfNotNull(changePrimaryConstructorAction, addConstructorAction)
    }

    override fun createAddPropertyActions(targetClass: JvmClass, request: MemberRequest.Property): List<IntentionAction> {
        val targetContainer = targetClass.toKtClassOrFile() ?: return emptyList()

        val modifierBuilder = ModifierBuilder(targetContainer).apply { addJvmModifier(request.visibilityModifier) }
        if (!modifierBuilder.isValid) return emptyList()

        val resolutionFacade = targetContainer.getResolutionFacade()
        val nullableAnyType = resolutionFacade.moduleDescriptor.builtIns.nullableAnyType
        val ktType = (request.propertyType as? PsiType)?.resolveToKotlinType(resolutionFacade) ?: nullableAnyType
        val propertyInfo = PropertyInfo(
                request.propertyName,
                TypeInfo.Empty,
                TypeInfo(ktType, Variance.INVARIANT),
                request.setterRequired,
                listOf(targetContainer),
                modifierList = modifierBuilder.modifierList,
                withInitializer = true
        )
        val propertyInfos = if (request.setterRequired) {
            listOf(propertyInfo, propertyInfo.copyProperty(isLateinitPreferred = true))
        }
        else {
            listOf(propertyInfo)
        }
        return propertyInfos.map { CreatePropertyFix(targetClass, targetContainer, it) }
    }

    override fun createAddFieldActions(targetClass: JvmClass, request: CreateFieldRequest): List<IntentionAction> {
        val targetContainer = targetClass.toKtClassOrFile() ?: return emptyList()

        val modifierBuilder = ModifierBuilder(targetContainer, allowJvmStatic = false).apply {
            addJvmModifiers(request.modifiers)
            addAnnotation(JVM_FIELD_ANNOTATION_FQ_NAME)
        }
        if (!modifierBuilder.isValid) return emptyList()

        val resolutionFacade = targetContainer.getResolutionFacade()
        val typeInfo = request.fieldType.toKotlinTypeInfo(resolutionFacade)
        val writable = JvmModifier.FINAL !in request.modifiers
        val propertyInfo = PropertyInfo(
                request.fieldName,
                TypeInfo.Empty,
                typeInfo,
                writable,
                listOf(targetContainer),
                isForCompanion = JvmModifier.STATIC in request.modifiers,
                modifierList = modifierBuilder.modifierList,
                withInitializer = true
        )
        val propertyInfos = if (writable) {
            listOf(propertyInfo, propertyInfo.copyProperty(isLateinitPreferred = true))
        }
        else {
            listOf(propertyInfo)
        }
        return propertyInfos.map { CreatePropertyFix(targetClass, targetContainer, it) }
    }

    override fun createAddMethodActions(targetClass: JvmClass, request: CreateMethodRequest): List<IntentionAction> {
        val targetContainer = targetClass.toKtClassOrFile() ?: return emptyList()

        val modifierBuilder = ModifierBuilder(targetContainer).apply { addJvmModifiers(request.modifiers) }
        if (!modifierBuilder.isValid) return emptyList()

        val resolutionFacade = targetContainer.getResolutionFacade()
        val returnTypeInfo = request.returnType.toKotlinTypeInfo(resolutionFacade)
        val parameterInfos = request.parameters.map { (suggestedNames, expectedTypes) ->
            ParameterInfo(expectedTypes.toKotlinTypeInfo(resolutionFacade), suggestedNames.names.toList())
        }
        val functionInfo = FunctionInfo(
                request.methodName,
                TypeInfo.Empty,
                returnTypeInfo,
                listOf(targetContainer),
                parameterInfos,
                isAbstract = JvmModifier.ABSTRACT in request.modifiers,
                isForCompanion = JvmModifier.STATIC in request.modifiers,
                modifierList = modifierBuilder.modifierList,
                preferEmptyBody = true
        )
        val action = object : CreateCallableFromUsageFix<KtElement>(targetContainer, listOf(functionInfo)) {
            override fun getFamilyName() = "Add method"
            override fun getText() = "Add method '${request.methodName}' to '${targetClass.name}'"
        }
        return listOf(action)
    }
}