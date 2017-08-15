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

package org.jetbrains.kotlin.idea.actions.generate

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMemberChooserObject
import org.jetbrains.kotlin.idea.core.overrideImplement.generateMember
import org.jetbrains.kotlin.idea.core.quoteIfNeeded
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import java.util.*

tailrec fun ClassDescriptor.findDeclaredFunction(
        name: String,
        checkSuperClasses: Boolean,
        filter: (FunctionDescriptor) -> Boolean
): FunctionDescriptor? {
    unsubstitutedMemberScope
            .getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_IDE)
            .firstOrNull { it.containingDeclaration == this && it.kind == CallableMemberDescriptor.Kind.DECLARATION && filter(it) }
            ?.let { return it }

    return if (checkSuperClasses) getSuperClassOrAny().findDeclaredFunction(name, checkSuperClasses, filter) else null
}

fun getPropertiesToUseInGeneratedMember(classOrObject: KtClassOrObject): List<KtNamedDeclaration> {
    return ArrayList<KtNamedDeclaration>().apply {
        classOrObject.primaryConstructorParameters.filterTo(this) { it.hasValOrVar() }
        classOrObject.declarations.filterIsInstance<KtProperty>().filterTo(this) {
            val descriptor = it.resolveToDescriptor()
            when (descriptor) {
                is ValueParameterDescriptor -> true
                is PropertyDescriptor -> descriptor.getter?.isDefault ?: true
                else -> false
            }
        }
    }.filter {
        KotlinNameSuggester.isIdentifier(it.name?.quoteIfNeeded())
    }
}

private val MEMBER_RENDERER = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.withOptions {
    modifiers = emptySet()
    startFromName = true
    parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
}

fun confirmMemberRewrite(targetClass: KtClass, vararg descriptors: FunctionDescriptor): Boolean {
    if (ApplicationManager.getApplication().isUnitTestMode) return true

    val functionsText = descriptors.joinToString(separator = " and ") { "'${MEMBER_RENDERER.render(it)}'" }
    val message = "Functions $functionsText are already defined\nfor class ${targetClass.name}. Do you want to delete them and proceed?"
    return Messages.showYesNoDialog(targetClass.project, message,
                                    CodeInsightBundle.message("generate.equals.and.hashcode.already.defined.title"),
                                    Messages.getQuestionIcon()) == Messages.YES
}

fun generateFunctionSkeleton(descriptor: FunctionDescriptor, targetClass: KtClassOrObject): KtNamedFunction {
    return OverrideMemberChooserObject
            .create(targetClass.project, descriptor, descriptor, OverrideMemberChooserObject.BodyType.EMPTY)
            .generateMember(targetClass, false) as KtNamedFunction
}