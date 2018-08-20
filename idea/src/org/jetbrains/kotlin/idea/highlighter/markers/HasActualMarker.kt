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
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.util.actualsForExpected
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.MultiTargetPlatform
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.getMultiTargetPlatform
import java.awt.event.MouseEvent

fun getPlatformActualTooltip(declaration: KtDeclaration): String? {
    val actualDeclarations = declaration.actualsForExpected()
    if (actualDeclarations.isEmpty()) return null

    return actualDeclarations.asSequence()
        .mapNotNull { it.toDescriptor()?.module }
        .groupBy { it.getMultiTargetPlatform() }
        .filter { (platform, _) -> platform != null }
        .entries
        .joinToString(prefix = "Has actuals in ") { (platform, modules) ->
            val modulesSuffix = if (modules.size <= 1) "" else " (${modules.size} modules)"
            when (platform) {
                is MultiTargetPlatform.Specific ->
                    "${platform.platform}$modulesSuffix"
                MultiTargetPlatform.Common ->
                    "common$modulesSuffix"
                null ->
                    throw AssertionError("Platform should not be null")
            }
        }
}

fun navigateToPlatformActual(e: MouseEvent?, declaration: KtDeclaration) {
    val actualDeclarations = declaration.actualsForExpected()
    if (actualDeclarations.isEmpty()) return

    val renderer = object : DefaultPsiElementCellRenderer() {
        override fun getContainerText(element: PsiElement?, name: String?) = ""
    }
    PsiElementListNavigator.openTargets(
        e,
        actualDeclarations.toTypedArray(),
        "Choose actual for ${declaration.name}",
        "Actuals for ${declaration.name}",
        renderer
    )
}