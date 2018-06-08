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

package org.jetbrains.kotlin.idea

import com.intellij.codeInsight.navigation.GotoTargetHandler
import com.intellij.codeInsight.navigation.GotoTargetRendererProvider
import com.intellij.ide.util.PsiElementListCellRenderer
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.idea.presentation.KtLightClassListCellRenderer
import org.jetbrains.kotlin.idea.presentation.DeclarationByModuleRenderer
import org.jetbrains.kotlin.idea.util.isEffectivelyActual
import org.jetbrains.kotlin.psi.KtDeclaration

class KotlinGotoTargetRenderProvider : GotoTargetRendererProvider {
    override fun getRenderer(element: PsiElement, gotoData: GotoTargetHandler.GotoData): PsiElementListCellRenderer<*>? {
        return when (element) {
            is KtLightClass -> {
                // Need to override default Java render
                KtLightClassListCellRenderer()
            }
            is KtDeclaration -> {
                if (element.isEffectivelyActual()) {
                    DeclarationByModuleRenderer()
                } else {
                    null
                }
            }
            else -> {
                null
            }
        }
    }
}
