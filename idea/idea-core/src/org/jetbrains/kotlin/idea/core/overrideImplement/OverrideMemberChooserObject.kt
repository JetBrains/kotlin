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

package org.jetbrains.kotlin.idea.core.overrideImplement

import com.intellij.codeInsight.generation.ClassMember
import com.intellij.codeInsight.generation.MemberChooserObject
import com.intellij.codeInsight.generation.MemberChooserObjectBase
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.TemplateKind
import org.jetbrains.kotlin.idea.core.getFunctionBodyTextFromTemplate
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMemberChooserObject.BodyType.*
import org.jetbrains.kotlin.idea.core.util.DescriptorMemberChooserObject
import org.jetbrains.kotlin.idea.j2k.IdeaDocCommentConverter
import org.jetbrains.kotlin.idea.kdoc.KDocElementFactory
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.approximateFlexibleTypes
import org.jetbrains.kotlin.idea.util.expectedDescriptors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.findDocComment.findDocComment
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRenderer.Companion.withOptions
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier.*
import org.jetbrains.kotlin.renderer.OverrideRenderingPolicy
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.checkers.ExperimentalUsageChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.setSingleOverridden
import org.jetbrains.kotlin.util.findCallableMemberBySignature

interface OverrideMemberChooserObject : ClassMember {
    sealed class BodyType(val requiresReturn: Boolean = true) {
        object NO_BODY : BodyType()
        object EMPTY_OR_TEMPLATE : BodyType(requiresReturn = false)
        object FROM_TEMPLATE : BodyType(requiresReturn = false)
        object SUPER : BodyType()
        object QUALIFIED_SUPER : BodyType()

        class Delegate(val receiverName: String) : BodyType()
    }

    val descriptor: CallableMemberDescriptor
    val immediateSuper: CallableMemberDescriptor
    val bodyType: BodyType
    val preferConstructorParameter: Boolean

    companion object {
        fun create(
            project: Project,
            descriptor: CallableMemberDescriptor,
            immediateSuper: CallableMemberDescriptor,
            bodyType: BodyType,
            preferConstructorParameter: Boolean = false
        ): OverrideMemberChooserObject {
            val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor)
            return if (declaration != null) {
                create(declaration, descriptor, immediateSuper, bodyType, preferConstructorParameter)
            } else {
                WithoutDeclaration(descriptor, immediateSuper, bodyType, preferConstructorParameter)
            }
        }

        fun create(
            declaration: PsiElement,
            descriptor: CallableMemberDescriptor,
            immediateSuper: CallableMemberDescriptor,
            bodyType: BodyType,
            preferConstructorParameter: Boolean = false
        ): OverrideMemberChooserObject =
            WithDeclaration(descriptor, declaration, immediateSuper, bodyType, preferConstructorParameter)

        private class WithDeclaration(
            descriptor: CallableMemberDescriptor,
            declaration: PsiElement,
            override val immediateSuper: CallableMemberDescriptor,
            override val bodyType: BodyType,
            override val preferConstructorParameter: Boolean
        ) : DescriptorMemberChooserObject(declaration, descriptor), OverrideMemberChooserObject {

            override val descriptor: CallableMemberDescriptor
                get() = super.descriptor as CallableMemberDescriptor
        }

        private class WithoutDeclaration(
            override val descriptor: CallableMemberDescriptor,
            override val immediateSuper: CallableMemberDescriptor,
            override val bodyType: BodyType,
            override val preferConstructorParameter: Boolean
        ) : MemberChooserObjectBase(
            DescriptorMemberChooserObject.getText(descriptor), DescriptorMemberChooserObject.getIcon(null, descriptor)
        ), OverrideMemberChooserObject {

            override fun getParentNodeDelegate(): MemberChooserObject? {
                val parentClassifier = descriptor.containingDeclaration as? ClassifierDescriptor ?: return null
                return MemberChooserObjectBase(
                    DescriptorMemberChooserObject.getText(parentClassifier), DescriptorMemberChooserObject.getIcon(null, parentClassifier)
                )
            }
        }
    }
}

enum class MemberGenerateMode {
    OVERRIDE,
    ACTUAL,
    EXPECT
}

fun OverrideMemberChooserObject.generateMember(
    targetClass: KtClassOrObject,
    copyDoc: Boolean
) = generateMember(targetClass, copyDoc, targetClass.project, mode = MemberGenerateMode.OVERRIDE)

fun OverrideMemberChooserObject.generateMember(
    targetClass: KtClassOrObject?,
    copyDoc: Boolean,
    project: Project,
    mode: MemberGenerateMode
): KtCallableDeclaration {
    val descriptor = immediateSuper

    val bodyType = when {
        targetClass?.hasExpectModifier() == true -> NO_BODY
        descriptor.extensionReceiverParameter != null && mode == MemberGenerateMode.OVERRIDE -> FROM_TEMPLATE
        else -> bodyType
    }

    val baseRenderer = when (mode) {
        MemberGenerateMode.OVERRIDE -> OVERRIDE_RENDERER
        MemberGenerateMode.ACTUAL -> ACTUAL_RENDERER
        MemberGenerateMode.EXPECT -> EXPECT_RENDERER
    }
    val renderer = baseRenderer.withOptions {
        if (descriptor is ClassConstructorDescriptor && descriptor.isPrimary) {
            val containingClass = descriptor.containingDeclaration
            if (containingClass.kind == ClassKind.ANNOTATION_CLASS || containingClass.isInline) {
                renderPrimaryConstructorParametersAsProperties = true
            }
        }
    }

    if (preferConstructorParameter && descriptor is PropertyDescriptor) {
        return generateConstructorParameter(project, descriptor, renderer, mode == MemberGenerateMode.OVERRIDE)
    }

    val newMember: KtCallableDeclaration = when (descriptor) {
        is FunctionDescriptor -> generateFunction(project, descriptor, renderer, bodyType, mode == MemberGenerateMode.OVERRIDE)
        is PropertyDescriptor -> generateProperty(project, descriptor, renderer, bodyType, mode == MemberGenerateMode.OVERRIDE)
        else -> error("Unknown member to override: $descriptor")
    }

    when (mode) {
        MemberGenerateMode.ACTUAL -> newMember.addModifier(KtTokens.ACTUAL_KEYWORD)
        MemberGenerateMode.EXPECT -> if (targetClass == null) {
            newMember.addModifier(KtTokens.EXPECT_KEYWORD)
        }
        MemberGenerateMode.OVERRIDE -> {
            if (targetClass?.hasActualModifier() == true) {
                val expectClassDescriptors =
                    targetClass.resolveToDescriptorIfAny()?.expectedDescriptors()?.filterIsInstance<ClassDescriptor>().orEmpty()
                if (expectClassDescriptors.any { expectClassDescriptor ->
                        val expectMemberDescriptor = expectClassDescriptor.findCallableMemberBySignature(immediateSuper)
                        expectMemberDescriptor?.isExpect == true && expectMemberDescriptor.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE
                    }
                ) {
                    newMember.addModifier(KtTokens.ACTUAL_KEYWORD)
                }
            }
        }
    }

    if (copyDoc) {
        val kDoc = when (val superDeclaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor)?.navigationElement) {
            is KtDeclaration ->
                findDocComment(superDeclaration)
            is PsiDocCommentOwner -> {
                val kDocText = superDeclaration.docComment?.let { IdeaDocCommentConverter.convertDocComment(it) }
                if (kDocText.isNullOrEmpty()) null else KDocElementFactory(project).createKDocFromText(kDocText)
            }
            else -> null
        }
        if (kDoc != null) {
            newMember.addAfter(kDoc, null)
        }
    }

    return newMember
}

private val OVERRIDE_RENDERER = withOptions {
    defaultParameterValueRenderer = null
    modifiers = setOf(OVERRIDE, ANNOTATIONS)
    withDefinedIn = false
    classifierNamePolicy = ClassifierNamePolicy.SOURCE_CODE_QUALIFIED
    overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OVERRIDE
    unitReturnType = false
    enhancedTypes = true
    typeNormalizer = IdeDescriptorRenderers.APPROXIMATE_FLEXIBLE_TYPES
    renderUnabbreviatedType = false
    annotationFilter = {
        val annotations = it.type.constructor.declarationDescriptor?.annotations
        annotations != null && (annotations.hasAnnotation(ExperimentalUsageChecker.REQUIRES_OPT_IN_FQ_NAME) ||
                annotations.hasAnnotation(ExperimentalUsageChecker.OLD_EXPERIMENTAL_FQ_NAME))
    }
    presentableUnresolvedTypes = true
    informativeErrorType = false
}


private val EXPECT_RENDERER = OVERRIDE_RENDERER.withOptions {
    modifiers = setOf(VISIBILITY, MODALITY, OVERRIDE, INNER, MEMBER_KIND)
    renderConstructorKeyword = false
    secondaryConstructorsAsPrimary = false
    renderDefaultVisibility = false
    renderDefaultModality = false
}

private val ACTUAL_RENDERER = EXPECT_RENDERER.withOptions {
    modifiers = modifiers + ACTUAL
    actualPropertiesInPrimaryConstructor = true
    renderConstructorDelegation = true
}

private fun PropertyDescriptor.wrap(forceOverride: Boolean): PropertyDescriptor {
    val delegate = copy(containingDeclaration, if (forceOverride) Modality.OPEN else modality, visibility, kind, true) as PropertyDescriptor
    val newDescriptor = object : PropertyDescriptor by delegate {
        override fun isExpect() = false
    }
    if (forceOverride) {
        newDescriptor.setSingleOverridden(this)
    }
    return newDescriptor
}

private fun FunctionDescriptor.wrap(forceOverride: Boolean): FunctionDescriptor {
    if (this is ClassConstructorDescriptor) return this.wrap()
    return object : FunctionDescriptor by this {
        override fun isExpect() = false
        override fun getModality() = if (forceOverride) Modality.OPEN else this@wrap.modality
        override fun getReturnType() = this@wrap.returnType?.approximateFlexibleTypes(preferNotNull = true, preferStarForRaw = true)
        override fun getOverriddenDescriptors() = if (forceOverride) listOf(this@wrap) else this@wrap.overriddenDescriptors
        override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D) =
            visitor.visitFunctionDescriptor(this, data)
    }
}

private fun ClassConstructorDescriptor.wrap(): ClassConstructorDescriptor {
    return object : ClassConstructorDescriptor by this {
        override fun isExpect() = false
        override fun getModality() = Modality.FINAL
        override fun getReturnType() = this@wrap.returnType.approximateFlexibleTypes(preferNotNull = true, preferStarForRaw = true)
        override fun getOverriddenDescriptors(): List<ClassConstructorDescriptor> = emptyList()
        override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D) =
            visitor.visitConstructorDescriptor(this, data)
    }
}

private fun generateProperty(
    project: Project,
    descriptor: PropertyDescriptor,
    renderer: DescriptorRenderer,
    bodyType: OverrideMemberChooserObject.BodyType,
    forceOverride: Boolean
): KtProperty {
    val newDescriptor = descriptor.wrap(forceOverride)

    val returnType = descriptor.returnType
    val returnsNotUnit = returnType != null && !KotlinBuiltIns.isUnit(returnType)

    val body =
        if (bodyType != NO_BODY) {
            buildString {
                append("\nget()")
                append(" = ")
                append(generateUnsupportedOrSuperCall(project, descriptor, bodyType, !returnsNotUnit))
                if (descriptor.isVar) {
                    append("\nset(value) {}")
                }
            }
        } else ""
    return KtPsiFactory(project).createProperty(renderer.render(newDescriptor) + body)
}

private fun generateConstructorParameter(
    project: Project,
    descriptor: PropertyDescriptor,
    renderer: DescriptorRenderer,
    forceOverride: Boolean
): KtParameter {
    val newDescriptor = descriptor.wrap(forceOverride)
    newDescriptor.setSingleOverridden(descriptor)
    return KtPsiFactory(project).createParameter(renderer.render(newDescriptor))
}

private fun generateFunction(
    project: Project,
    descriptor: FunctionDescriptor,
    renderer: DescriptorRenderer,
    bodyType: OverrideMemberChooserObject.BodyType,
    forceOverride: Boolean
): KtFunction {
    val newDescriptor = descriptor.wrap(forceOverride)

    val returnType = descriptor.returnType
    val returnsNotUnit = returnType != null && !KotlinBuiltIns.isUnit(returnType)

    val body = if (bodyType != NO_BODY) {
        val delegation = generateUnsupportedOrSuperCall(project, descriptor, bodyType, !returnsNotUnit)
        val returnPrefix = if (returnsNotUnit && bodyType.requiresReturn) "return " else ""
        "{$returnPrefix$delegation\n}"
    } else ""

    val factory = KtPsiFactory(project)
    val functionText = renderer.render(newDescriptor) + body
    return when (descriptor) {
        is ClassConstructorDescriptor -> {
            if (descriptor.isPrimary) {
                factory.createPrimaryConstructor(functionText)
            } else {
                factory.createSecondaryConstructor(functionText)
            }
        }
        else -> factory.createFunction(functionText)
    }
}

private fun OverrideMemberChooserObject.BodyType.effectiveBodyType(canBeEmpty: Boolean): OverrideMemberChooserObject.BodyType =
    if (!canBeEmpty && this == EMPTY_OR_TEMPLATE) FROM_TEMPLATE else this

fun generateUnsupportedOrSuperCall(
    project: Project,
    descriptor: CallableMemberDescriptor,
    bodyType: OverrideMemberChooserObject.BodyType,
    canBeEmpty: Boolean = true
): String {
    when (bodyType.effectiveBodyType(canBeEmpty)) {
        EMPTY_OR_TEMPLATE -> return ""
        FROM_TEMPLATE -> {
            val templateKind = if (descriptor is FunctionDescriptor) TemplateKind.FUNCTION else TemplateKind.PROPERTY_INITIALIZER
            return getFunctionBodyTextFromTemplate(
                project,
                templateKind,
                descriptor.name.asString(),
                descriptor.returnType?.let { IdeDescriptorRenderers.SOURCE_CODE.renderType(it) } ?: "Unit",
                null
            )
        }
        else -> return buildString {
            if (bodyType is Delegate) {
                append(bodyType.receiverName)
            } else {
                append("super")
                if (bodyType == QUALIFIED_SUPER) {
                    val superClassFqName = IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(
                        descriptor.containingDeclaration as ClassifierDescriptor
                    )
                    append("<").append(superClassFqName).append(">")
                }
            }
            append(".").append(descriptor.name.render())

            if (descriptor is FunctionDescriptor) {
                val paramTexts = descriptor.valueParameters.map {
                    val renderedName = it.name.render()
                    if (it.varargElementType != null) "*$renderedName" else renderedName
                }
                paramTexts.joinTo(this, prefix = "(", postfix = ")")
            }
        }
    }
}

fun KtModifierListOwner.makeNotActual() {
    removeModifier(KtTokens.ACTUAL_KEYWORD)
    removeModifier(KtTokens.IMPL_KEYWORD)
}

fun KtModifierListOwner.makeActual() {
    addModifier(KtTokens.ACTUAL_KEYWORD)
}