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
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.addConstructorKeyword
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionFactory
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionForFirstParentOfType
import org.jetbrains.kotlin.psi.JetAnnotation
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetPrimaryConstructor
import org.jetbrains.kotlin.psi.JetPsiFactory

public class DeprecatedAnnotationSyntaxFix(element: JetAnnotation) : JetIntentionAction<JetAnnotation>(element), CleanupFix {
    override fun getFamilyName(): String = "Replace with '@' annotations"
    override fun getText(): String = "Replace with '@' annotations"

    override fun invoke(project: Project, editor: Editor?, file: JetFile?) = replaceWithAtAnnotationEntries(element)

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? =
                diagnostic.createIntentionForFirstParentOfType(::DeprecatedAnnotationSyntaxFix)

        public fun createWholeProjectFixFactory(): JetSingleIntentionActionFactory = createIntentionFactory {
            JetWholeProjectForEachElementOfTypeFix.createByPredicate<JetAnnotation>(
                    predicate = { it.isDeprecated() },
                    taskProcessor = { replaceWithAtAnnotationEntries(it) },
                    name = "Replace with '@' annotations in whole project"
            )
        }

        private fun replaceWithAtAnnotationEntries(annotation: JetAnnotation) {
            val psiFactory = JetPsiFactory(annotation)

            val hasFileKeyword = annotation.hasFileKeyword()

            val parent = annotation.getParent()
            val owner = parent.getParent()
            var prevElement: PsiElement = annotation

            for (entry in annotation.getEntries()) {
                val newEntry = if (hasFileKeyword) createFileAnnotationEntry(psiFactory, entry.getText())
                               else psiFactory.createAnnotationEntry("@" + entry.getText())
                val added = parent.addAfter(newEntry, prevElement)

                if (prevElement != annotation) {
                    parent.addBefore(psiFactory.createWhiteSpace(), added)
                }

                prevElement = added
            }

            if (owner is JetPrimaryConstructor) {
                owner.addConstructorKeyword()
            }

            annotation.delete()
        }

        private fun createFileAnnotationEntry(psiFactory: JetPsiFactory, text: String) =
                psiFactory.createFile("@file:$text").getAnnotationEntries().first()
    }
}
