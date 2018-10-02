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

package org.jetbrains.kotlin.idea.highlighter.markers

import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator
import com.intellij.ide.util.DefaultPsiElementCellRenderer
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.idea.caches.project.implementedDescriptors
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.util.expectedDeclarationIfAny
import org.jetbrains.kotlin.idea.util.hasDeclarationOf
import org.jetbrains.kotlin.psi.KtDeclaration
import java.awt.event.MouseEvent

fun getExpectedDeclarationTooltip(declaration: KtDeclaration): String? {
    val descriptor = declaration.toDescriptor() as? MemberDescriptor ?: return null
    val platformModuleDescriptor = declaration.containingKtFile.findModuleDescriptor()

    val commonModuleDescriptors = platformModuleDescriptor.implementedDescriptors
    if (!commonModuleDescriptors.any { it.hasDeclarationOf(descriptor) }) return null

    return "Has declaration in common module"
}

fun navigateToExpectedDeclaration(e: MouseEvent?, declaration: KtDeclaration) {
    val expectedDeclarations =
        listOfNotNull(declaration.expectedDeclarationIfAny()) +
                declaration.findMarkerBoundDeclarations().mapNotNull { it.expectedDeclarationIfAny() }
    if (expectedDeclarations.isEmpty()) return

    val renderer = object : DefaultPsiElementCellRenderer() {
        override fun getContainerText(element: PsiElement?, name: String?) = ""
    }
    PsiElementListNavigator.openTargets(
        e,
        expectedDeclarations.toTypedArray(),
        "Choose expected for ${declaration.name}",
        "Expected for ${declaration.name}",
        renderer
    )
}