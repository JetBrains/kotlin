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

import com.intellij.psi.NavigatablePsiElement
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.ModuleProductionSourceInfo
import org.jetbrains.kotlin.idea.caches.resolve.ModuleTestSourceInfo
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.MultiTargetPlatform
import org.jetbrains.kotlin.resolve.checkers.HeaderImplDeclarationChecker
import org.jetbrains.kotlin.resolve.getMultiTargetPlatform

fun ModuleDescriptor.commonModuleOrNull(): ModuleDescriptor? {
    val sourceKind = sourceKind
    return allDependencyModules.firstOrNull { dependency ->
        dependency.getMultiTargetPlatform() == MultiTargetPlatform.Common && dependency.sourceKind == sourceKind
    }
}

private val ModuleDescriptor.sourceKind: SourceKind
    get() = when (getCapability(ModuleInfo.Capability)) {
        is ModuleProductionSourceInfo -> SourceKind.PRODUCTION
        is ModuleTestSourceInfo -> SourceKind.TEST
        else -> SourceKind.NONE
    }

private enum class SourceKind { NONE, PRODUCTION, TEST }

fun ModuleDescriptor.hasDeclarationOf(descriptor: MemberDescriptor) = declarationOf(descriptor) != null

private fun ModuleDescriptor.declarationOf(descriptor: MemberDescriptor): DeclarationDescriptor? =
        with (HeaderImplDeclarationChecker(this)) {
            when (descriptor) {
                is CallableMemberDescriptor ->
                    descriptor.findNamesakesFromTheSameModule().filter { it.isHeader }.firstOrNull()
                is ClassifierDescriptorWithTypeParameters ->
                    descriptor.findClassifiersFromTheSameModule().filter { it is ClassDescriptor && it.isHeader }.firstOrNull()
                else ->
                    null
            }
        }

fun getHeaderDeclarationTooltip(declaration: KtDeclaration): String? {
    val descriptor = declaration.toDescriptor() as? MemberDescriptor ?: return null
    val platformModuleDescriptor = declaration.containingKtFile.findModuleDescriptor()

    val commonModuleDescriptor = platformModuleDescriptor.commonModuleOrNull() ?: return null
    if (!commonModuleDescriptor.hasDeclarationOf(descriptor)) return null

    return "Has declaration in common module"
}

fun navigateToHeaderDeclaration(declaration: KtDeclaration) {
    val descriptor = declaration.toDescriptor() as? MemberDescriptor ?: return
    val platformModuleDescriptor = declaration.containingKtFile.findModuleDescriptor()

    val commonModuleDescriptor = platformModuleDescriptor.commonModuleOrNull() ?: return
    val headerDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(
            commonModuleDescriptor.declarationOf(descriptor) ?: return
    ) as? NavigatablePsiElement ?: return

    headerDeclaration.navigate(false)
}
