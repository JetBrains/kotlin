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
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionForFirstParentOfType
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.utils.addIfNotNull

public class MigrateAnnotationMethodCallFix(
        expression: JetCallExpression
) : JetIntentionAction<JetCallExpression>(expression) {
    override fun getText() = "Replace method call with property access"
    override fun getFamilyName() = getText()

    override fun invoke(project: Project, editor: Editor?, file: JetFile) = replaceWithSimpleCall(element)

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) = diagnostic.createIntentionForFirstParentOfType(::MigrateAnnotationMethodCallFix)
        fun replaceWithSimpleCall(expression: JetCallExpression) {
            val simpleName = expression.getCalleeExpression()?.getText() ?: return
            expression.replace(JetPsiFactory(expression).createSimpleName(simpleName))
        }
    }
}

class MigrateAnnotationMethodCallInWholeFile : IntentionAction {
    override fun getText() = "Replace deprecated method calls with property access in current file"
    override fun getFamilyName() = getText()

    override fun startInWriteAction(): Boolean = true
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) = true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) = (file as? JetFile)?.let {
            it.analyzeFully().getDiagnostics().
            filter { it.getFactory() == ErrorsJvm.DEPRECATED_ANNOTATION_METHOD_CALL }.
            forEach {
                (it.getPsiElement() as? JetCallExpression)?.let { MigrateAnnotationMethodCallFix.replaceWithSimpleCall(it) }
            }
    } ?: Unit

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) = MigrateAnnotationMethodCallInWholeFile()
    }
}
