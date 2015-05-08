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
import org.jetbrains.kotlin.JetNodeTypes
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionFactory
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionForFirstParentOfType
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes
import org.jetbrains.kotlin.resolve.DeclarationsChecker

class DeprecatedEnumEntrySuperConstructorSyntaxFix(element: JetEnumEntry): JetIntentionAction<JetEnumEntry>(element) {
    override fun getFamilyName(): String = getText()

    override fun getText(): String = "Change to short enum entry super constructor"

    override fun invoke(project: Project, editor: Editor?, file: JetFile?) = changeConstructorToShort(element)

    companion object: JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction?  =
                diagnostic.createIntentionForFirstParentOfType(::DeprecatedEnumEntrySuperConstructorSyntaxFix)

        public fun createWholeProjectFixFactory(): JetSingleIntentionActionFactory = createIntentionFactory {
            JetWholeProjectForEachElementOfTypeFix.createByPredicate<JetEnumEntry>(
                    predicate = { DeclarationsChecker.enumEntryUsesDeprecatedSuperConstructor(it) },
                    taskProcessor = { changeConstructorToShort(it) },
                    modalTitle = "Replacing deprecated enum constructor syntax",
                    name = "Change to short enum entry super constructor in the whole project",
                    familyName = "Change to short enum entry super constructor in the whole project"
            )
        }

        private fun transformInitializerList(list: JetInitializerList) {
            val psiFactory = JetPsiFactory(list)
            val userType = list.getInitializers()[0].getTypeReference()!!.getTypeElement() as JetUserType
            userType.getReferenceExpression()!!.replace(psiFactory.createEnumEntrySuperclassReferenceExpression())
        }

        private fun changeConstructorToShort(entry: JetEnumEntry) {
            val list = entry.getInitializerList()!!
            transformInitializerList(list)
            // Delete everything between name and initializer (colon with whitespaces)
            entry.deleteChildRange(entry.getFirstChild().getNextSibling(), list.getPrevSibling())
        }
    }
}