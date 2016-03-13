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
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.util.DescriptorMemberChooserObject
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.*
import org.jetbrains.kotlin.resolve.descriptorUtil.setSingleOverridden

interface OverrideMemberChooserObject : ClassMember {
    enum class BodyType {
        EMPTY,
        SUPER,
        QUALIFIED_SUPER
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
            if (declaration != null) {
                return WithDeclaration(descriptor, declaration, immediateSuper, bodyType, preferConstructorParameter)
            }
            else {
                return WithoutDeclaration(descriptor, immediateSuper, bodyType, preferConstructorParameter)
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

fun OverrideMemberChooserObject.generateMember(project: Project): KtCallableDeclaration {
    val descriptor = immediateSuper
    if (preferConstructorParameter && descriptor is PropertyDescriptor) return generateConstructorParameter(project, descriptor)

    return when (descriptor) {
        is SimpleFunctionDescriptor -> generateFunction(project, descriptor, bodyType)
        is PropertyDescriptor -> generateProperty(project, descriptor, bodyType)
        else -> error("Unknown member to override: $descriptor")
    }
}

private val OVERRIDE_RENDERER = DescriptorRenderer.withOptions {
    renderDefaultValues = false
    modifiers = setOf(DescriptorRendererModifier.OVERRIDE)
    withDefinedIn = false
    classifierNamePolicy = ClassifierNamePolicy.SOURCE_CODE_QUALIFIED
    overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OVERRIDE
    unitReturnType = false
    typeNormalizer = IdeDescriptorRenderers.APPROXIMATE_FLEXIBLE_TYPES
}

private fun generateProperty(project: Project, descriptor: PropertyDescriptor, bodyType: OverrideMemberChooserObject.BodyType): KtProperty {
    val newDescriptor = descriptor.copy(descriptor.containingDeclaration, Modality.OPEN, descriptor.visibility,
                                        descriptor.kind, /* copyOverrides = */ true) as PropertyDescriptor
    newDescriptor.setSingleOverridden(descriptor)

    val body = buildString {
        append("\nget()")
        append(" = ")
        append(generateUnsupportedOrSuperCall(descriptor, bodyType))
        if (descriptor.isVar) {
            append("\nset(value) {}")
        }
    }
    return KtPsiFactory(project).createProperty(OVERRIDE_RENDERER.render(newDescriptor) + body)
}

private fun generateConstructorParameter(project: Project, descriptor: PropertyDescriptor): KtParameter {
    val newDescriptor = descriptor.copy(descriptor.containingDeclaration, Modality.OPEN, descriptor.visibility,
                                        descriptor.kind, /* copyOverrides = */ true) as PropertyDescriptor
    newDescriptor.setSingleOverridden(descriptor)
    return KtPsiFactory(project).createParameter(OVERRIDE_RENDERER.render(newDescriptor))
}

private fun generateFunction(project: Project, descriptor: FunctionDescriptor, bodyType: OverrideMemberChooserObject.BodyType): KtNamedFunction {
    val newDescriptor = descriptor.copy(descriptor.containingDeclaration, Modality.OPEN, descriptor.visibility,
                                        descriptor.kind, /* copyOverrides = */ true)
    newDescriptor.setSingleOverridden(descriptor)

    val returnType = descriptor.returnType
    val returnsNotUnit = returnType != null && !KotlinBuiltIns.isUnit(returnType)

    val delegation = generateUnsupportedOrSuperCall(descriptor, bodyType)

    val body = "{" + (if (returnsNotUnit && bodyType != OverrideMemberChooserObject.BodyType.EMPTY) "return " else "") + delegation + "}"

    return KtPsiFactory(project).createFunction(OVERRIDE_RENDERER.render(newDescriptor) + body)
}

fun generateUnsupportedOrSuperCall(descriptor: CallableMemberDescriptor, bodyType: OverrideMemberChooserObject.BodyType): String {
    if (bodyType == OverrideMemberChooserObject.BodyType.EMPTY) {
        return "throw UnsupportedOperationException()"
    }
    else {
        return buildString {
            append("super")
            if (bodyType == OverrideMemberChooserObject.BodyType.QUALIFIED_SUPER) {
                val superClassFqName = IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(descriptor.containingDeclaration as ClassifierDescriptor)
                append("<").append(superClassFqName).append(">")
            }
            append(".").append(descriptor.name.render())

            if (descriptor is FunctionDescriptor) {
                val paramTexts = descriptor.valueParameters.map {
                    val renderedName = it.name.render()
                    if (it.varargElementType != null) "*$renderedName" else renderedName
                }
                paramTexts.joinTo(this, prefix="(", postfix=")")
            }
        }
    }
}


