/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionFactory
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetPsiFactory

public class DeprecatedTraitSyntaxFix(element: PsiElement): JetIntentionAction<PsiElement>(element) {
    override fun getFamilyName() = "Replace with 'interface'"
    override fun getText() = getFamilyName()

    override fun invoke(project: Project, editor: Editor?, file: JetFile?)
            = replaceWithInterfaceKeyword(element)


    companion object : JetSingleIntentionActionFactory() {
        fun replaceWithInterfaceKeyword(element: PsiElement) {
            val cls = JetPsiFactory(element.getProject()).createClass("interface A {}")
            element.replace(cls.getNode().findChildByType(JetTokens.INTERFACE_KEYWORD)!!.getPsi())
        }

        override fun createAction(diagnostic: Diagnostic): IntentionAction =
                DeprecatedTraitSyntaxFix(diagnostic.getPsiElement())

        public fun createWholeProjectFixFactory(): JetSingleIntentionActionFactory = createIntentionFactory {
            JetWholeProjectForEachElementOfTypeFix.createByPredicate<JetClass>(
                    predicate = { it.getNode().findChildByType(JetTokens.TRAIT_KEYWORD) != null },
                    taskProcessor = { replaceWithInterfaceKeyword(it.getNode().findChildByType(JetTokens.TRAIT_KEYWORD).getPsi())},
                    modalTitle = "Replacing deprecated trait syntax",
                    name = "Replace with 'interface' in whole project",
                    familyName = "Replace with 'interface' in whole project"
            )
        }
    }
}
