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
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionForFirstParentOfType
import org.jetbrains.kotlin.load.kotlin.getJavaAnnotationCallValueArgumentsThatShouldBeNamed
import org.jetbrains.kotlin.psi.JetAnnotationEntry
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument

public class ReplaceJavaAnnotationPositionedArgumentsFix(element: JetAnnotationEntry)
: JetIntentionAction<JetAnnotationEntry>(element), CleanupFix {
    override fun getText(): String  = "Replace invalid positioned arguments for annotation"
    override fun getFamilyName(): String = getText()

    override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        val resolvedCall = element.getResolvedCall(element.analyze()) ?: return
        val psiFactory = JetPsiFactory(project)

        getJavaAnnotationCallValueArgumentsThatShouldBeNamed(resolvedCall).forEach argumentProcessor@{
            argument ->
            val valueArgument = (argument.value as? ExpressionValueArgument)?.getValueArgument() ?: return@argumentProcessor
            val expression = valueArgument.getArgumentExpression() ?: return@argumentProcessor

            valueArgument.asElement().replace(psiFactory.createArgument(expression, argument.getKey().getName()))
        }
    }

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) =
                diagnostic.createIntentionForFirstParentOfType(::ReplaceJavaAnnotationPositionedArgumentsFix)
    }
}
