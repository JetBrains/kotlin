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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.cfg.pseudocode.containingDeclarationForPseudocode
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class AddSuspendModifierFix(element: KtModifierListOwner): AddModifierFix(element, KtTokens.SUSPEND_KEYWORD) {

    override fun getText() =
            when (element) {
                is KtNamedFunction -> "Make ${element?.name ?: "containing function"} suspend"
                else -> super.getText()
            }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val element = diagnostic.psiElement
            val function = (element as? KtElement)?.containingDeclarationForPseudocode as? KtNamedFunction ?: return null

            return AddSuspendModifierFix(function)
        }
    }
}