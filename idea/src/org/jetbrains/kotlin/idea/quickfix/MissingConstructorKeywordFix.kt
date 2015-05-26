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
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionFactory
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionForFirstParentOfType
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetPrimaryConstructor
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.addConstructorKeyword

public class MissingConstructorKeywordFix(element: JetPrimaryConstructor) : JetIntentionAction<JetPrimaryConstructor>(element), CleanupFix {
    override fun getFamilyName(): String = getText()
    override fun getText(): String = "Add 'constructor' keyword"

    override fun invoke(project: Project, editor: Editor?, file: JetFile?) {
        element.addConstructorKeyword()
    }

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? =
                diagnostic.createIntentionForFirstParentOfType(::MissingConstructorKeywordFix)

        public fun createWholeProjectFixFactory(): JetSingleIntentionActionFactory = createIntentionFactory {
            JetWholeProjectForEachElementOfTypeFix.createByPredicate<JetPrimaryConstructor>(
                    predicate = { it.getModifierList() != null && !it.hasConstructorKeyword() },
                    taskProcessor = { it.addConstructorKeyword() },
                    modalTitle = "Adding missing 'constructor' keyword",
                    name = "Add missing 'constructor' keyword in whole project",
                    familyName = "Add missing 'constructor' keyword in whole project"
            )
        }
    }
}
