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

import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator
import com.intellij.ide.util.DefaultPsiElementCellRenderer
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.highlighter.allImplementingCompatibleModules
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.MultiTargetPlatform
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.getMultiTargetPlatform
import java.awt.event.MouseEvent

fun ModuleDescriptor.hasActualsFor(descriptor: MemberDescriptor) =
        actualsFor(descriptor).isNotEmpty()

fun ModuleDescriptor.actualsFor(descriptor: MemberDescriptor, checkCompatible: Boolean = true): List<DeclarationDescriptor> =
        with(ExpectedActualDeclarationChecker) {
            if (checkCompatible) {
                descriptor.findCompatibleActualForExpected(this@actualsFor)
            }
            else {
                descriptor.findAnyActualForExpected(this@actualsFor)
            }
        }

fun getPlatformActualTooltip(declaration: KtDeclaration): String? {
    val descriptor = declaration.toDescriptor() as? MemberDescriptor ?: return null
    val commonModuleDescriptor = declaration.containingKtFile.findModuleDescriptor()

    val platformModulesWithActuals = commonModuleDescriptor.allImplementingCompatibleModules.filter {
        it.hasActualsFor(descriptor)
    }
    if (platformModulesWithActuals.isEmpty()) return null

    return platformModulesWithActuals.joinToString(prefix = "Has actuals in ") {
        (it.getMultiTargetPlatform() as MultiTargetPlatform.Specific).platform
    }
}

fun navigateToPlatformActual(e: MouseEvent?, declaration: KtDeclaration) {
    val actuals = declaration.actualsForExpected()
    if (actuals.isEmpty()) return

    val renderer = DefaultPsiElementCellRenderer()
    PsiElementListNavigator.openTargets(e,
                                        actuals.toTypedArray(),
                                        "Choose actual for ${declaration.name}",
                                        "Actuals for ${declaration.name}",
                                        renderer)
}

private fun DeclarationDescriptor.actualsForExpected(): Collection<DeclarationDescriptor> {
    if (this is MemberDescriptor) {
        if (!this.isExpect) return emptyList()

        return module.allImplementingCompatibleModules.flatMap { it.actualsFor(this) }
    }

    if (this is ValueParameterDescriptor) {
        return containingDeclaration.actualsForExpected().mapNotNull { (it as? CallableDescriptor)?.valueParameters?.getOrNull(index) }
    }

    return emptyList()
}

internal fun KtDeclaration.actualsForExpected(): Set<KtDeclaration> {
    return unsafeResolveToDescriptor().actualsForExpected().mapNotNullTo(LinkedHashSet()) {
        DescriptorToSourceUtils.descriptorToDeclaration(it) as? KtDeclaration
    }
}