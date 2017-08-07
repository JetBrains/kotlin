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

package org.jetbrains.kotlin.idea.highlighter.markers

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.highlighter.sourceKind
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.MultiTargetPlatform
import org.jetbrains.kotlin.resolve.checkers.HeaderImplDeclarationChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.getMultiTargetPlatform

fun ModuleDescriptor.commonModuleOrNull(): ModuleDescriptor? {
    val sourceKind = sourceKind
    return allDependencyModules.firstOrNull { dependency ->
        dependency.getMultiTargetPlatform() == MultiTargetPlatform.Common && dependency.sourceKind == sourceKind
    }
}

fun ModuleDescriptor.hasDeclarationOf(descriptor: MemberDescriptor) = declarationOf(descriptor) != null

private fun ModuleDescriptor.declarationOf(descriptor: MemberDescriptor): DeclarationDescriptor? =
        with(HeaderImplDeclarationChecker) {
            descriptor.findCompatibleHeaderForImpl(this@declarationOf).firstOrNull()
        }

fun getHeaderDeclarationTooltip(declaration: KtDeclaration): String? {
    val descriptor = declaration.toDescriptor() as? MemberDescriptor ?: return null
    val platformModuleDescriptor = declaration.containingKtFile.findModuleDescriptor()

    val commonModuleDescriptor = platformModuleDescriptor.commonModuleOrNull() ?: return null
    if (!commonModuleDescriptor.hasDeclarationOf(descriptor)) return null

    return "Has declaration in common module"
}

fun navigateToHeaderDeclaration(declaration: KtDeclaration) {
    declaration.headerDeclarationIfAny()?.navigate(false)
}

internal fun MemberDescriptor.headerDescriptor() = module.commonModuleOrNull()?.declarationOf(this)

internal fun KtDeclaration.headerDeclarationIfAny(): KtDeclaration? {
    val headerDescriptor = (toDescriptor() as? MemberDescriptor)?.headerDescriptor() ?: return null
    return DescriptorToSourceUtils.descriptorToDeclaration(headerDescriptor) as? KtDeclaration
}

internal fun KtDeclaration.isHeaderOrHeaderClassMember() =
        hasModifier(KtTokens.HEADER_KEYWORD) || (containingClassOrObject?.hasModifier(KtTokens.HEADER_KEYWORD) ?: false)

private fun DeclarationDescriptor.liftToHeader(): DeclarationDescriptor? {
    if (this is MemberDescriptor) {
        return when {
            isHeader -> this
            isImpl -> headerDescriptor()
            else -> null
        }
    }

    if (this is ValueParameterDescriptor) {
        val containingHeaderDescriptor = containingDeclaration.liftToHeader() as? CallableDescriptor ?: return null
        return containingHeaderDescriptor.valueParameters.getOrNull(index)
    }

    return null
}

internal fun KtDeclaration.liftToHeader(): KtDeclaration? {
    val descriptor = resolveToDescriptor()
    val headerDescriptor = descriptor.liftToHeader() ?: return null
    return DescriptorToSourceUtils.descriptorToDeclaration(headerDescriptor) as? KtDeclaration
}