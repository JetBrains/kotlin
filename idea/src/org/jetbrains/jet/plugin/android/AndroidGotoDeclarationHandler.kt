/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.android

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.psi.PsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.ServiceManager
import org.jetbrains.jet.lang.resolve.android.AndroidUIXmlProcessor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.PsiReferenceExpression

public class AndroidGotoDeclarationHandler : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(sourceElement: PsiElement?, offset: Int, editor: Editor?): Array<PsiElement>? {
        // FIXME: find a way to filter out more bad sourceElements before they hit idToXmlAttribute() resulting in cache rebuild
        if (sourceElement is LeafPsiElement) {
            val refExp = PsiTreeUtil.getParentOfType(sourceElement, javaClass<PsiReferenceExpression>())
            val parser = ServiceManager.getService(sourceElement.getProject(), javaClass<AndroidUIXmlProcessor>())
            val psiElement = parser?.resourceManager?.idToXmlAttribute(sourceElement.getText())
            if (psiElement != null) {
                return array(psiElement)
            }
            else return null
        }
        else return null
    }

    override fun getActionText(context: DataContext?): String? {
        return null
    }
}
