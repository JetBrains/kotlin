/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl
import com.intellij.psi.util.PropertyUtil
import com.intellij.psi.util.PropertyUtilBase
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.descriptors.resolveClassByFqName
import org.jetbrains.kotlin.idea.KotlinBundle
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
import org.jetbrains.kotlin.idea.util.resolveToKotlinType
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType
import org.jetbrains.kotlin.resolve.AnnotationChecker
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.descriptorUtil.module
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
        override fun getFamilyName() = KotlinBundle.message("add.property")
        override fun getText(): String {
            val info = callableInfos.first() as PropertyInfo
            return buildString {
                append(KotlinBundle.message("text.add"))
                if (info.isLateinitPreferred || info.modifierList?.hasModifier(KtTokens.LATEINIT_KEYWORD) == true) {
                    append("lateinit ")
                }
                append(if (info.writable) "var" else "val")
                append(KotlinBundle.message("property.0.to.1", info.name, classOrFileName.toString()))
            }
        }
    }

    private fun JvmClass.toKtClassOrFile(): KtElement? = when (val psi = sourceElement) {
        is KtClassOrObject -> psi
        is KtLightClassForSourceDeclaration -> psi.kotlinOrigin
        is KtLightClassForFacade -> psi.files.firstOrNull()
        else -> null
    }

    private inline fun <reified T : KtElement> JvmElement.toKtElement() = sourceElement?.unwrapped as? T

    private fun fakeParametersExpressions(parameters: List<ExpectedParameter>, project: Project): Array<PsiExpression>? = when {
        parameters.isEmpty() -> emptyArray()
        else -> JavaPsiFacade
            .getElementFactory(project)
            .createParameterList(
                parameters.map { it.semanticNames.firstOrNull() }.toTypedArray(),
                parameters.map {
                    it.expectedTypes.firstOrNull()?.theType
                        ?.let { JvmPsiConversionHelper.getInstance(project).convertType(it) } ?: return null
                }.toTypedArray()
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

    override fun createChangeModifierActions(target: JvmModifiersOwner, request: ChangeModifierRequest): List<IntentionAction> {
        val kModifierOwner = target.toKtElement<KtModifierListOwner>() ?: return emptyList()

        val modifier = request.modifier
        val shouldPresent = request.shouldBePresent()
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
        val parameters = request.expectedParameters
        val parameterInfos = parameters.mapIndexed { index, param ->
            val ktType = param.expectedTypes.firstOrNull()?.theType?.let { helper.convertType(it).resolveToKotlinType(resolutionFacade) }
                ?: nullableAnyType
            val name = param.semanticNames.firstOrNull() ?: "arg${index + 1}"
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
            override fun getFamilyName() = KotlinBundle.message("add.method")
            override fun getText() = KotlinBundle.message(
                "add.0.constructor.to.1",
                if (needPrimary) KotlinBundle.message("text.primary") else KotlinBundle.message("text.secondary"),
                targetClassName.toString()
            )
        }

        val changePrimaryConstructorAction = run {
            val primaryConstructor = targetKtClass.primaryConstructor ?: return@run null
            val lightMethod = primaryConstructor.toLightMethods().firstOrNull() ?: return@run null
            val project = targetKtClass.project
            val fakeParametersExpressions = fakeParametersExpressions(parameters, project) ?: return@run null
            QuickFixFactory.getInstance().createChangeMethodSignatureFromUsageFix(
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
        } else {
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
        } else {
            listOf(propertyInfo(false))
        }
        return propertyInfos.map { CreatePropertyFix(targetContainer, it, targetClass.name) }
    }

    override fun createAddMethodActions(targetClass: JvmClass, request: CreateMethodRequest): List<IntentionAction> {
        val targetContainer = targetClass.toKtClassOrFile() ?: return emptyList()

        val modifierBuilder = ModifierBuilder(targetContainer).apply { addJvmModifiers(request.modifiers) }
        if (!modifierBuilder.isValid) return emptyList()

        val resolutionFacade = KotlinCacheService.getInstance(targetContainer.project)
            .getResolutionFacadeByFile(targetContainer.containingFile, JvmPlatforms.unspecifiedJvmPlatform) ?: return emptyList()
        val returnTypeInfo = request.returnType.toKotlinTypeInfo(resolutionFacade)
        val parameters = request.expectedParameters
        val parameterInfos = parameters.map { parameter ->
            ParameterInfo(parameter.expectedTypes.toKotlinTypeInfo(resolutionFacade), parameter.semanticNames.toList())
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
            override fun getFamilyName() = KotlinBundle.message("add.method")
            override fun getText() = KotlinBundle.message("add.method.0.to.1", methodName, targetClassName.toString())
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
