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
import com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionFactory
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionForFirstParentOfType
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes
import org.jetbrains.kotlin.resolve.DeclarationsChecker

class DeprecatedEnumEntryDelimiterSyntaxFix(element: JetEnumEntry): JetIntentionAction<JetEnumEntry>(element) {

    override fun getFamilyName(): String = getText()

    override fun getText(): String = "Insert lacking comma(s) / semicolon(s)"

    override fun invoke(project: Project, editor: Editor?, file: JetFile?) = insertLackingCommaSemicolon(element)

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? =
                diagnostic.createIntentionForFirstParentOfType(::DeprecatedEnumEntryDelimiterSyntaxFix)

        public fun createWholeProjectFixFactory(): JetSingleIntentionActionFactory = createIntentionFactory {
            JetWholeProjectForEachElementOfTypeFix.createByPredicate<JetEnumEntry>(
                    predicate = { DeclarationsChecker.enumEntryUsesDeprecatedOrNoDelimiter(it) },
                    taskProcessor = { insertLackingCommaSemicolon(it) },
                    modalTitle = "Replacing deprecated enum entry delimiter syntax",
                    name = "Insert lacking comma(s) / semicolon(s) in the whole project",
                    familyName = "Insert lacking comma(s) / semicolon(s) in the whole project"
            )
        }

        private fun insertLackingCommaSemicolon(enumEntry: JetEnumEntry) {
            val body = enumEntry.getParent() as JetClassBody
            val entries = body.getChildrenOfType<JetEnumEntry>()
            val psiFactory = JetPsiFactory(body)
            for ((entryIndex, entry) in entries.withIndex()) {
                var next = entry.getNextSiblingIgnoringWhitespaceAndComments()
                var nextType = next?.getNode()?.getElementType()
                if (entryIndex < entries.size() - 1) {
                    if (next is PsiErrorElement && next.getFirstChild()?.getNode()?.getElementType() == JetTokens.SEMICOLON) {
                        // Fix for syntax error like ENUM_ENTRY1; ENUM_ENTRY2; ENUM_ENTRY3
                        next.replace(psiFactory.createComma())
                    }
                    else if (nextType != JetTokens.COMMA) {
                        // Classic case like ENUM_ENTRY1 ENUM_ENTRY2
                        body.addAfter(psiFactory.createComma(), entry)
                    }
                }
                else {
                    if (nextType == JetTokens.COMMA) {
                        // ENUM_ENTRY_LAST, fun foo()
                        next!!.replace(psiFactory.createSemicolon())
                    }
                    else if (nextType != JetTokens.SEMICOLON && nextType != JetTokens.RBRACE) {
                        // ENUM_ENTRY_LAST fun foo()
                        body.addAfter(psiFactory.createSemicolon(), entry)
                    }
                }
            }
        }
    }
}