/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions.generate

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMemberChooserObject
import org.jetbrains.kotlin.idea.core.overrideImplement.generateMember
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isIdentifier
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import java.util.*

tailrec fun ClassDescriptor.findDeclaredFunction(
    name: String,
    checkSuperClasses: Boolean,
    filter: (FunctionDescriptor) -> Boolean
): FunctionDescriptor? {
    unsubstitutedMemberScope.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_IDE)
        .firstOrNull { it.containingDeclaration == this && it.kind == CallableMemberDescriptor.Kind.DECLARATION && filter(it) }
        ?.let { return it }

    return if (checkSuperClasses) getSuperClassOrAny().findDeclaredFunction(name, checkSuperClasses, filter) else null
}

fun getPropertiesToUseInGeneratedMember(classOrObject: KtClassOrObject): List<KtNamedDeclaration> {
    return ArrayList<KtNamedDeclaration>().apply {
        classOrObject.primaryConstructorParameters.filterTo(this) { it.hasValOrVar() }
        classOrObject.declarations.asSequence().filterIsInstance<KtProperty>().filterTo(this) {
            val descriptor = it.unsafeResolveToDescriptor()
            when (descriptor) {
                is ValueParameterDescriptor, is PropertyDescriptor -> true
                else -> false
            }
        }
    }.filter {
        it.name?.quoteIfNeeded().isIdentifier()
    }
}

private val MEMBER_RENDERER = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.withOptions {
    modifiers = emptySet()
    startFromName = true
    parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
}

fun confirmMemberRewrite(targetClass: KtClass, vararg descriptors: FunctionDescriptor): Boolean {
    if (ApplicationManager.getApplication().isUnitTestMode) return true

    val functionsText = descriptors.joinToString(separator = " ${KotlinBundle.message("configuration.text.and")} ") { "'${MEMBER_RENDERER.render(it)}'" }
    val message = KotlinBundle.message("action.generate.functions.already.defined", functionsText, targetClass.name.toString())
    return Messages.showYesNoDialog(
        targetClass.project, message,
        CodeInsightBundle.message("generate.equals.and.hashcode.already.defined.title"),
        Messages.getQuestionIcon()
    ) == Messages.YES
}

fun generateFunctionSkeleton(descriptor: FunctionDescriptor, targetClass: KtClassOrObject): KtNamedFunction {
    return OverrideMemberChooserObject
        .create(targetClass.project, descriptor, descriptor, OverrideMemberChooserObject.BodyType.FROM_TEMPLATE)
        .generateMember(targetClass, false) as KtNamedFunction
}