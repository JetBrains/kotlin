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

import com.intellij.codeInsight.daemon.QuickFixBundle
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.QuickFixFactory
import com.intellij.lang.java.beans.PropertyKind
import com.intellij.lang.jvm.*
import com.intellij.lang.jvm.actions.*
import com.intellij.lang.jvm.types.JvmType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.psi.*
import com.intellij.psi.codeStyle.SuggestedNameInfo
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl
import com.intellij.psi.util.PropertyUtil
import com.intellij.psi.util.PropertyUtilBase
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.MutablePackageFragmentDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.appendModifier
import org.jetbrains.kotlin.idea.quickfix.AddModifierFix
import org.jetbrains.kotlin.idea.quickfix.RemoveModifierFix
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable.CreateCallableFromUsageFix
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.approximateFlexibleTypes
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME
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
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType
import org.jetbrains.kotlin.resolve.AnnotationChecker
import org.jetbrains.kotlin.resolve.DefaultBuiltInPlatforms
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.storage.LockBasedStorageManager
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
            contextElement: KtElement,
        propertyInfo: PropertyInfo,
        private val classOrFileName: String?
    ) : CreateCallableFromUsageFix<KtElement>(contextElement, listOf(propertyInfo)) {
        override fun getFamilyName() = "Add property"
        override fun getText(): String {
            val info = callableInfos.first() as PropertyInfo
            return buildString {
                append("Add '")
                if (info.isLateinitPreferred || info.modifierList?.hasModifier(KtTokens.LATEINIT_KEYWORD) == true) {
                    append("lateinit ")
                }
                append(if (info.writable) "var" else "val")
                append("' property '${info.name}' to '$classOrFileName'")
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

    private fun fakeParametersExpressions(parameters: List<Pair<SuggestedNameInfo, List<ExpectedType>>>, project: Project): Array<PsiExpression>? =
            when {
                parameters.isEmpty() -> emptyArray()
                else -> JavaPsiFacade
                        .getElementFactory(project)
                        .createParameterList(
                            parameters.map { it.first.names.firstOrNull() }.toTypedArray(),
                            parameters.map { JvmPsiConversionHelper.getInstance(project).asPsiType(it) ?: return null }.toTypedArray()
                        )
                        .parameters
                        .map(::FakeExpressionFromParameter)
                        .toTypedArray()
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
        //TODO: make similar to `createAddMethodActions`
        val (kToken, shouldPresentMapped) = when {
            modifier == JvmModifier.FINAL -> KtTokens.OPEN_KEYWORD to !shouldPresent
            modifier == JvmModifier.PUBLIC && shouldPresent ->
                kModifierOwner.visibilityModifierType()
                    ?.takeIf { it != KtTokens.DEFAULT_VISIBILITY_KEYWORD }
                    ?.let { it to false } ?: return emptyList()
            else -> javaPsiModifiersMapping[modifier] to shouldPresent
        }
        if (kToken == null) return emptyList()

        val action = if (shouldPresentMapped)
            AddModifierFix.createIfApplicable(kModifierOwner, kToken)
        else
            RemoveModifierFix(kModifierOwner, kToken, false)
        return listOfNotNull(action)
    }

    override fun createAddConstructorActions(targetClass: JvmClass, request: CreateConstructorRequest): List<IntentionAction> {
        val targetKtClass = targetClass.toKtClassOrFile() as? KtClass ?: return emptyList()

        val modifierBuilder = ModifierBuilder(targetKtClass).apply { addJvmModifiers(request.modifiers) }
        if (!modifierBuilder.isValid) return emptyList()
        val resolutionFacade = targetKtClass.getResolutionFacade()
        val nullableAnyType = resolutionFacade.moduleDescriptor.builtIns.nullableAnyType
        val helper = JvmPsiConversionHelper.getInstance(targetKtClass.project)
        val parameters = request.parameters as List<Pair<SuggestedNameInfo, List<ExpectedType>>>
        val parameterInfos = parameters.mapIndexed { index, param: Pair<SuggestedNameInfo, List<ExpectedType>> ->
            val ktType = helper.asPsiType(param)?.resolveToKotlinType(resolutionFacade) ?: nullableAnyType
            val name = param.first.names.firstOrNull() ?: "arg${index + 1}"
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
        val targetClassName = targetClass.name
        val addConstructorAction = object : CreateCallableFromUsageFix<KtElement>(targetKtClass, listOf(constructorInfo)) {
            override fun getFamilyName() = "Add method"
            override fun getText() = "Add ${if (needPrimary) "primary" else "secondary"} constructor to '$targetClassName'"
        }

        val changePrimaryConstructorAction = run {
            val primaryConstructor = targetKtClass.primaryConstructor ?: return@run null
            val lightMethod = primaryConstructor.toLightMethods().firstOrNull() ?: return@run null
            val project = targetKtClass.project
            val fakeParametersExpressions = fakeParametersExpressions(parameters, project) ?: return@run null
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
        return createAddPropertyActions(
            targetContainer, listOf(request.visibilityModifier),
            request.propertyType, request.propertyName, request.setterRequired, targetClass.name
        )
    }

    private fun createAddPropertyActions(
        targetContainer: KtElement,
        modifiers: Iterable<JvmModifier>,
        propertyType: JvmType,
        propertyName: String,
        setterRequired: Boolean,
        classOrFileName: String?
    ): List<IntentionAction> {
        val modifierBuilder = ModifierBuilder(targetContainer).apply { addJvmModifiers(modifiers) }
        if (!modifierBuilder.isValid) return emptyList()

        val resolutionFacade = targetContainer.getResolutionFacade()
        val nullableAnyType = resolutionFacade.moduleDescriptor.builtIns.nullableAnyType

        val ktType = (propertyType as? PsiType)?.resolveToKotlinType(resolutionFacade) ?: nullableAnyType
        val propertyInfo = PropertyInfo(
            propertyName,
                TypeInfo.Empty,
                TypeInfo(ktType, Variance.INVARIANT),
            setterRequired,
                listOf(targetContainer),
                modifierList = modifierBuilder.modifierList,
                withInitializer = true
        )
        val propertyInfos = if (setterRequired) {
            listOf(propertyInfo, propertyInfo.copyProperty(isLateinitPreferred = true))
        }
        else {
            listOf(propertyInfo)
        }
        return propertyInfos.map { CreatePropertyFix(targetContainer, it, classOrFileName) }
    }

    override fun createAddFieldActions(targetClass: JvmClass, request: CreateFieldRequest): List<IntentionAction> {
        val targetContainer = targetClass.toKtClassOrFile() ?: return emptyList()

        val resolutionFacade = targetContainer.getResolutionFacade()
        val typeInfo = request.fieldType.toKotlinTypeInfo(resolutionFacade)
        val writable = JvmModifier.FINAL !in request.modifiers

        fun propertyInfo(lateinit: Boolean) = PropertyInfo(
            request.fieldName,
            TypeInfo.Empty,
            typeInfo,
            writable,
            listOf(targetContainer),
            isLateinitPreferred = false, // Dont set it to `lateinit` because it works via templates that brings issues in batch field adding
            isForCompanion = JvmModifier.STATIC in request.modifiers,
            modifierList = ModifierBuilder(targetContainer, allowJvmStatic = false).apply {
                addJvmModifiers(request.modifiers)
                if (modifierList.children.none { it.node.elementType in KtTokens.VISIBILITY_MODIFIERS })
                    addJvmModifier(JvmModifier.PUBLIC)
                if (lateinit)
                    modifierList.appendModifier(KtTokens.LATEINIT_KEYWORD)
                if (!request.modifiers.contains(JvmModifier.PRIVATE) && !lateinit)
                    addAnnotation(JVM_FIELD_ANNOTATION_FQ_NAME)
            }.modifierList,
            withInitializer = !lateinit
        )

        val propertyInfos = if (writable) {
            listOf(propertyInfo(false), propertyInfo(true))
        }
        else {
            listOf(propertyInfo(false))
        }
        return propertyInfos.map { CreatePropertyFix(targetContainer, it, targetClass.name) }
    }

    override fun createAddMethodActions(targetClass: JvmClass, request: CreateMethodRequest): List<IntentionAction> {
        val targetContainer = targetClass.toKtClassOrFile() ?: return emptyList()

        val modifierBuilder = ModifierBuilder(targetContainer).apply { addJvmModifiers(request.modifiers) }
        if (!modifierBuilder.isValid) return emptyList()

        val resolutionFacade = KotlinCacheService.getInstance(targetContainer.project)
            .getResolutionFacadeByFile(targetContainer.containingFile, DefaultBuiltInPlatforms.jvmPlatform) ?: return emptyList()
        val returnTypeInfo = request.returnType.toKotlinTypeInfo(resolutionFacade)
        val parameters = request.parameters as List<Pair<SuggestedNameInfo, List<ExpectedType>>>
        val parameterInfos = parameters.map { (suggestedNames, expectedTypes) ->
            ParameterInfo(expectedTypes.toKotlinTypeInfo(resolutionFacade), suggestedNames.names.toList())
        }
        val methodName = request.methodName
        val functionInfo = FunctionInfo(
            methodName,
            TypeInfo.Empty,
            returnTypeInfo,
            listOf(targetContainer),
            parameterInfos,
            isForCompanion = JvmModifier.STATIC in request.modifiers,
            modifierList = modifierBuilder.modifierList,
            preferEmptyBody = true
        )
        val targetClassName = targetClass.name
        val action = object : CreateCallableFromUsageFix<KtElement>(targetContainer, listOf(functionInfo)) {
            override fun getFamilyName() = "Add method"
            override fun getText() = "Add method '$methodName' to '$targetClassName'"
        }

        val nameAndKind = PropertyUtilBase.getPropertyNameAndKind(methodName) ?: return listOf(action)

        val propertyType = (request.expectedParameters.singleOrNull()?.expectedTypes ?: request.returnType)
            .firstOrNull { JvmPsiConversionHelper.getInstance(targetContainer.project).convertType(it.theType) != PsiType.VOID }
            ?: return listOf(action)

        return createAddPropertyActions(
            targetContainer,
            request.modifiers,
            propertyType.theType,
            nameAndKind.first,
            nameAndKind.second == PropertyKind.SETTER,
            targetClass.name
        )

    }

    override fun createAddAnnotationActions(target: JvmModifiersOwner, request: AnnotationRequest): List<IntentionAction> {
        val declaration = (target as? KtLightElement<*, *>)?.kotlinOrigin as? KtModifierListOwner ?: return emptyList()
        if (declaration.language != KotlinLanguage.INSTANCE) return emptyList()
        val annotationUseSiteTarget = when (target) {
            is JvmField -> AnnotationUseSiteTarget.FIELD
            is JvmMethod -> when {
                PropertyUtil.isSimplePropertySetter(target as? PsiMethod) -> AnnotationUseSiteTarget.PROPERTY_SETTER
                PropertyUtil.isSimplePropertyGetter(target as? PsiMethod) -> AnnotationUseSiteTarget.PROPERTY_GETTER
                else -> null
            }
            else -> null
        }
        return listOf(CreateAnnotationAction(declaration, annotationUseSiteTarget, request))
    }

    private class CreateAnnotationAction(
        target: KtModifierListOwner,
        val annotationTarget: AnnotationUseSiteTarget?,
        val request: AnnotationRequest
    ) : IntentionAction {

        private val pointer = target.createSmartPointer()

        override fun startInWriteAction(): Boolean = true

        override fun getText(): String =
            QuickFixBundle.message("create.annotation.text", StringUtilRt.getShortName(request.qualifiedName))

        override fun getFamilyName(): String = QuickFixBundle.message("create.annotation.family")

        override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = pointer.element != null


        override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
            val target = pointer.element ?: return
            val entry = addAnnotationEntry(target, request, annotationTarget)
            ShortenReferences.DEFAULT.process(entry)
        }

    }

    override fun createChangeParametersActions(target: JvmMethod, request: ChangeParametersRequest): List<IntentionAction> {
        val ktNamedFunction = (target as? KtLightElement<*, *>)?.kotlinOrigin as? KtNamedFunction ?: return emptyList()
        return listOfNotNull(ChangeMethodParameters.create(ktNamedFunction, request))
    }
}

internal fun addAnnotationEntry(
    target: KtModifierListOwner,
    request: AnnotationRequest,
    annotationTarget: AnnotationUseSiteTarget?
): KtAnnotationEntry {
    val annotationClass = JavaPsiFacade.getInstance(target.project).findClass(request.qualifiedName, target.resolveScope)

    val kotlinAnnotation = annotationClass?.language == KotlinLanguage.INSTANCE

    val annotationUseSiteTargetPrefix = run prefixEvaluation@{
        if (annotationTarget == null) return@prefixEvaluation ""

        val moduleDescriptor = (target as? KtDeclaration)?.resolveToDescriptorIfAny()?.module ?: return@prefixEvaluation ""
        val annotationClassDescriptor = moduleDescriptor.resolveClassByFqName(
            FqName(request.qualifiedName), NoLookupLocation.FROM_IDE
        ) ?: return@prefixEvaluation ""

        val applicableTargetSet =
            AnnotationChecker.applicableTargetSet(annotationClassDescriptor) ?: KotlinTarget.DEFAULT_TARGET_SET

        if (KotlinTarget.PROPERTY !in applicableTargetSet) return@prefixEvaluation ""

        "${annotationTarget.renderName}:"
    }

    // could be generated via descriptor when KT-30478 is fixed
    val entry = target.addAnnotationEntry(
        KtPsiFactory(target)
            .createAnnotationEntry(
                "@$annotationUseSiteTargetPrefix${request.qualifiedName}${
                request.attributes.takeIf { it.isNotEmpty() }?.mapIndexed { i, p ->
                    if (!kotlinAnnotation && i == 0 && p.name == "value")
                        renderAttributeValue(p.value).toString()
                    else
                        "${p.name} = ${renderAttributeValue(p.value)}"
                }?.joinToString(", ", "(", ")") ?: ""
                }"
            )
    )
    return entry
}

private fun renderAttributeValue(annotationAttributeRequest: AnnotationAttributeValueRequest) =
    when (annotationAttributeRequest) {
        is AnnotationAttributeValueRequest.PrimitiveValue -> annotationAttributeRequest.value
        is AnnotationAttributeValueRequest.StringValue -> "\"" + annotationAttributeRequest.value + "\""
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


internal fun PsiType.resolveToKotlinType(resolutionFacade: ResolutionFacade): KotlinType? {
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
        false,
        LockBasedStorageManager.NO_LOCKS
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


private fun JvmPsiConversionHelper.asPsiType(param: Pair<SuggestedNameInfo, List<ExpectedType>>): PsiType? =
    param.second.firstOrNull()?.theType?.let { convertType(it) }
