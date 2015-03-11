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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createVariable

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.intentions.ConvertToBlockBodyAction
import org.jetbrains.kotlin.idea.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.CreateFromUsageFixBase
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElement
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import java.util.Collections

object CreateLocalVariableActionFactory: JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val refExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetSimpleNameExpression>()) ?: return null
        if (refExpr.getQualifiedElement() != refExpr) return null

        val propertyName = refExpr.getReferencedName()

        val container = refExpr.parents(false)
                .filter { it is JetBlockExpression || it is JetDeclarationWithBody }
                .firstOrNull() as? JetElement ?: return null

        return object: CreateFromUsageFixBase(refExpr) {
            override fun getText(): String = JetBundle.message("create.local.variable.from.usage", propertyName)

            override fun invoke(project: Project, editor: Editor, file: JetFile) {
                val assignment = refExpr.getAssignmentByLHS()
                val varExpected = assignment != null
                var originalElement = assignment ?: refExpr

                val actualContainer = when (container) {
                    is JetBlockExpression -> container
                    else -> ConvertToBlockBodyAction.convert(container as JetDeclarationWithBody).getBodyExpression()!!
                } as JetBlockExpression

                if (actualContainer != container) {
                    val bodyExpression = actualContainer.getStatements().first() as JetExpression
                    originalElement = (bodyExpression as? JetReturnExpression)?.getReturnedExpression() ?: bodyExpression
                }

                val typeInfo = TypeInfo(
                        originalElement.getExpressionForTypeGuess(),
                        if (varExpected) Variance.INVARIANT else Variance.OUT_VARIANCE
                )
                val propertyInfo = PropertyInfo(propertyName, TypeInfo.Empty, typeInfo, varExpected, Collections.singletonList(actualContainer))

                with (CallableBuilderConfiguration(propertyInfo.singletonOrEmptyList(), originalElement, file, editor).createBuilder()) {
                    placement = CallablePlacement.NoReceiver(actualContainer)
                    CommandProcessor.getInstance().executeCommand(project, { build() }, getText(), null)
                }
            }
        }
    }
}
