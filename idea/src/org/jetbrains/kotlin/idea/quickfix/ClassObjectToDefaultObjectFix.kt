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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.project.PluginJetFilesProvider
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionFactory
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import java.util.ArrayList

public class ClassObjectToCompanionObjectFix(private val elem: JetObjectDeclaration) : JetIntentionAction<JetObjectDeclaration>(elem) {
    override fun getText(): String = JetBundle.message("migrate.class.object.to.companion")

    override fun getFamilyName(): String = JetBundle.message("migrate.class.object.to.companion.family")

    override fun invoke(project: Project, editor: Editor, file: JetFile) {
        changeClassKeywordToCompanionModifier(elem)
    }

    companion object Factory : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) =
                (diagnostic.getPsiElement() as? JetObjectDeclaration)?.let { ClassObjectToCompanionObjectFix(it) }

        public fun createWholeProjectFixFactory(): JetSingleIntentionActionFactory = createIntentionFactory {
            JetWholeProjectForEachElementOfTypeFix.createByPredicate<JetObjectDeclaration>(
                predicate = { it.getClassKeyword() != null },
                taskProcessor = { changeClassKeywordToCompanionModifier(it) },
                modalTitle = JetBundle.message("migrate.class.object.to.companion.in.whole.project.modal.title"),
                name = JetBundle.message("migrate.class.object.to.companion.in.whole.project"),
                familyName = JetBundle.message("migrate.class.object.to.companion.in.whole.project.family")
            )
        }

        private fun changeClassKeywordToCompanionModifier(objectDeclaration: JetObjectDeclaration) {
            objectDeclaration.getClassKeyword()?.delete()
            if (!objectDeclaration.hasModifier(JetTokens.COMPANION_KEYWORD)) {
                objectDeclaration.addModifier(JetTokens.COMPANION_KEYWORD)
            }
        }
    }
}
