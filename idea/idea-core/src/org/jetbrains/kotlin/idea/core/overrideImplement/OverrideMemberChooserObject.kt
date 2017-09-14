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
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.TemplateKind
import org.jetbrains.kotlin.idea.core.getFunctionBodyTextFromTemplate
import org.jetbrains.kotlin.idea.core.util.DescriptorMemberChooserObject
import org.jetbrains.kotlin.idea.j2k.IdeaDocCommentConverter
import org.jetbrains.kotlin.idea.kdoc.KDocElementFactory
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.approximateFlexibleTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.findDocComment.findDocComment
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.renderer.*
import org.jetbrains.kotlin.resolve.descriptorUtil.setSingleOverridden

interface OverrideMemberChooserObject : ClassMember {
    sealed class BodyType {
        object NO_BODY : BodyType()
        object EMPTY : BodyType()
        object SUPER : BodyType()
        object QUALIFIED_SUPER : BodyType()

        class Delegate(val receiverName: String) : BodyType()
    }

    val descriptor: CallableMemberDescriptor
    val immediateSuper: CallableMemberDescriptor
    val bodyType: BodyType
    val preferConstructorParameter: Boolean

    companion object {
        fun create(project: Project,
                   descriptor: CallableMemberDescriptor,
                   immediateSuper: CallableMemberDescriptor,
                   bodyType: BodyType,
                   preferConstructorParameter: Boolean = false
        ): OverrideMemberChooserObject {
            val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor)
            return if (declaration != null) {
                WithDeclaration(descriptor, declaration, immediateSuper, bodyType, preferConstructorParameter)
            }
            else {
                WithoutDeclaration(descriptor, immediateSuper, bodyType, preferConstructorParameter)
            }
        }

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
        ) : MemberChooserObjectBase(DescriptorMemberChooserObject.getText(descriptor), DescriptorMemberChooserObject.getIcon(null, descriptor)), OverrideMemberChooserObject {

            override fun getParentNodeDelegate(): MemberChooserObject? {
                val parentClassifier = descriptor.containingDeclaration as? ClassifierDescriptor ?: return null
                return MemberChooserObjectBase(DescriptorMemberChooserObject.getText(parentClassifier), DescriptorMemberChooserObject.getIcon(null, parentClassifier))
            }
        }
    }
}

fun OverrideMemberChooserObject.generateMember(targetClass: KtClassOrObject, copyDoc: Boolean): KtCallableDeclaration {
    val project = targetClass.project

    val bodyType = if (targetClass.hasExpectModifier()) OverrideMemberChooserObject.BodyType.NO_BODY else bodyType

    val descriptor = immediateSuper
    if (preferConstructorParameter && descriptor is PropertyDescriptor) return generateConstructorParameter(project, descriptor)

    val newMember: KtCallableDeclaration = when (descriptor) {
        is SimpleFunctionDescriptor -> generateFunction(project, descriptor, bodyType)
        is PropertyDescriptor -> generateProperty(project, descriptor, bodyType)
        else -> error("Unknown member to override: $descriptor")
    }

    if (!targetClass.hasModifier(KtTokens.IMPL_KEYWORD)) {
        newMember.removeModifier(KtTokens.IMPL_KEYWORD)
    }

    if (copyDoc) {
        val superDeclaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor)?.navigationElement
        val kDoc = when (superDeclaration) {
            is KtDeclaration ->
                findDocComment(superDeclaration)
            is PsiDocCommentOwner -> {
                val kDocText = superDeclaration.docComment?.let { IdeaDocCommentConverter.convertDocComment(it) }
                if (kDocText.isNullOrEmpty()) null else KDocElementFactory(project).createKDocFromText(kDocText!!)
            }
            else -> null
        }
        if (kDoc != null) {
            newMember.addAfter(kDoc, null)
        }
    }

    return newMember
}

private val OVERRIDE_RENDERER = DescriptorRenderer.withOptions {
    defaultParameterValueRenderer = null
    modifiers = setOf(DescriptorRendererModifier.OVERRIDE)
    withDefinedIn = false
    classifierNamePolicy = ClassifierNamePolicy.SOURCE_CODE_QUALIFIED
    overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OVERRIDE
    unitReturnType = false
    typeNormalizer = IdeDescriptorRenderers.APPROXIMATE_FLEXIBLE_TYPES
    renderUnabbreviatedType = false
}

private fun PropertyDescriptor.wrap(): PropertyDescriptor {
    val delegate = copy(containingDeclaration, Modality.OPEN, visibility, kind, true) as PropertyDescriptor
    val newDescriptor = object : PropertyDescriptor by delegate {
        override fun isExpect() = false
    }
    newDescriptor.setSingleOverridden(this)
    return newDescriptor
}

private fun FunctionDescriptor.wrap(): FunctionDescriptor {
    return object : FunctionDescriptor by this {
        override fun isExpect() = false
        override fun getModality() = Modality.OPEN
        override fun getReturnType() = this@wrap.returnType?.approximateFlexibleTypes(preferNotNull = true, preferStarForRaw = true)
        override fun getOverriddenDescriptors() = listOf(this@wrap)
        override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D) = visitor.visitFunctionDescriptor(this, data)
    }
}

private fun generateProperty(project: Project, descriptor: PropertyDescriptor, bodyType: OverrideMemberChooserObject.BodyType): KtProperty {
    val newDescriptor = descriptor.wrap()
    val body = buildString {
        append("\nget()")
        append(" = ")
        append(generateUnsupportedOrSuperCall(project, descriptor, bodyType))
        if (descriptor.isVar) {
            append("\nset(value) {}")
        }
    }
    return KtPsiFactory(project).createProperty(OVERRIDE_RENDERER.render(newDescriptor) + body)
}

private fun generateConstructorParameter(project: Project, descriptor: PropertyDescriptor): KtParameter {
    val newDescriptor = descriptor.wrap()
    newDescriptor.setSingleOverridden(descriptor)
    return KtPsiFactory(project).createParameter(OVERRIDE_RENDERER.render(newDescriptor))
}

private fun generateFunction(project: Project, descriptor: FunctionDescriptor, bodyType: OverrideMemberChooserObject.BodyType): KtNamedFunction {
    val newDescriptor = descriptor.wrap()

    val returnType = descriptor.returnType
    val returnsNotUnit = returnType != null && !KotlinBuiltIns.isUnit(returnType)

    val body = if (bodyType != OverrideMemberChooserObject.BodyType.NO_BODY) {
        val delegation = generateUnsupportedOrSuperCall(project, descriptor, bodyType)
        "{" + (if (returnsNotUnit && bodyType != OverrideMemberChooserObject.BodyType.EMPTY) "return " else "") + delegation + "\n}"
    }
    else ""

    return KtPsiFactory(project).createFunction(OVERRIDE_RENDERER.render(newDescriptor) + body)
}

fun generateUnsupportedOrSuperCall(
        project: Project,
        descriptor: CallableMemberDescriptor,
        bodyType: OverrideMemberChooserObject.BodyType
): String {
    if (bodyType == OverrideMemberChooserObject.BodyType.EMPTY) {
        val templateKind = if (descriptor is FunctionDescriptor) TemplateKind.FUNCTION else TemplateKind.PROPERTY_INITIALIZER
        return getFunctionBodyTextFromTemplate(project,
                                               templateKind,
                                               descriptor.name.asString(),
                                               descriptor.returnType?.let { IdeDescriptorRenderers.SOURCE_CODE.renderType(it) } ?: "Unit",
                                               null)
    }
    else {
        return buildString {
            if (bodyType is OverrideMemberChooserObject.BodyType.Delegate) {
                append(bodyType.receiverName)
            }
            else {
                append("super")
                if (bodyType == OverrideMemberChooserObject.BodyType.QUALIFIED_SUPER) {
                    val superClassFqName = IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(descriptor.containingDeclaration as ClassifierDescriptor)
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


