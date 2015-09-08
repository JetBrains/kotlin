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

package org.jetbrains.kotlin.idea.quickfix.migration

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.core.CommentSaver
import org.jetbrains.kotlin.idea.quickfix.CleanupFix
import org.jetbrains.kotlin.idea.quickfix.JetIntentionAction
import org.jetbrains.kotlin.idea.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

public class ReplaceAnnotationWithModifierFix(
        element: JetAnnotationEntry,
        private val replacement: JetModifierKeywordToken
) : JetIntentionAction<JetAnnotationEntry>(element), CleanupFix {
    override fun getFamilyName() = "Replace with modifier"
    override fun getText() = "Replace with '${replacement.value}'"

    override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        val psiFactory = JetPsiFactory(project)
        val modifier = psiFactory.createModifier(replacement)

        val parent = element.parent
        if (parent !is JetAnnotation) {
            val commentSaver = CommentSaver(element, saveLineBreaks = true)
            val result = element.replace(modifier)
            commentSaver.restore(result)
        }
        else {
            // within annotation list
            val modifierListOwner = (parent.parent?.parent as? JetModifierListOwner) ?: return
            // insert modifier
            modifierListOwner.addModifier(replacement)

            parent.removeEntry(element)
        }
    }

    companion object Factory : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val annotationEntry = diagnostic.psiElement.getNonStrictParentOfType<JetAnnotationEntry>() ?: return null
            val modifierValue = Errors.DEPRECATED_ANNOTATION_THAT_BECOMES_MODIFIER.cast(diagnostic).a

            return ReplaceAnnotationWithModifierFix(
                    annotationEntry, JetTokens.ANNOTATION_MODIFIERS_KEYWORDS_ARRAY.first() { it.value == modifierValue }
            )
        }
    }
}
