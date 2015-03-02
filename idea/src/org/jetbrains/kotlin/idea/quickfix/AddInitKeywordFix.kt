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

import org.jetbrains.kotlin.psi.*
import kotlin.platform.*
import org.jetbrains.kotlin.diagnostics.*
import com.intellij.codeInsight.intention.*
import org.jetbrains.kotlin.idea.*
import com.intellij.openapi.project.*
import com.intellij.openapi.editor.*
import com.intellij.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*


public class AddInitKeywordFix(element: JetClassInitializer) : JetIntentionAction<JetClassInitializer>(element) {
    override fun getText() = JetBundle.message("add.init.keyword")

    override fun getFamilyName() = JetBundle.message("add.init.keyword")

    override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        val initializer = element.copy() as JetClassInitializer
        val aClass = JetPsiFactory(file).createClass("""class A {
                ${initializer.getModifierList()?.getText() ?: ""} init ${initializer.getBody().getText()}
                }"""
        )
        val newInitializers = aClass.getAnonymousInitializers()
        assert(newInitializers.size() == 1)
        element.replace(newInitializers[0])
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return super.isAvailable(project, editor, file)
    }

    class object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            return AddInitKeywordFix(diagnostic.getPsiElement().getNonStrictParentOfType<JetClassInitializer>() ?: return null)
        }
    }
}
