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
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.intentions.ConvertToBlockBodyIntention
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.CreateFromUsageFixBase
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElement
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.types.Variance
import java.util.*

object CreateLocalVariableActionFactory : KotlinSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val refExpr = QuickFixUtil.getParentElementOfType(diagnostic, KtNameReferenceExpression::class.java) ?: return null
        if (refExpr.getQualifiedElement() != refExpr) return null
        if (refExpr.getParentOfTypeAndBranch<KtCallableReferenceExpression> { callableReference } != null) return null

        val propertyName = refExpr.getReferencedName()

        val container = refExpr.parents
            .filter { it is KtBlockExpression || it is KtDeclarationWithBody && it.bodyExpression != null }
            .firstOrNull() as? KtElement ?: return null

        return object : CreateFromUsageFixBase<KtSimpleNameExpression>(refExpr) {
            override fun getText(): String = KotlinBundle.message("create.local.variable.from.usage", propertyName)

            override fun invoke(project: Project, editor: Editor?, file: KtFile) {
                val assignment = refExpr.getAssignmentByLHS()
                val varExpected = assignment != null
                var originalElement: KtExpression = assignment ?: refExpr

                val actualContainer = when (container) {
                    is KtBlockExpression -> container
                    else -> ConvertToBlockBodyIntention.convert(container as KtDeclarationWithBody).bodyExpression!!
                } as KtBlockExpression

                if (actualContainer != container) {
                    val bodyExpression = actualContainer.statements.first()!!
                    originalElement = (bodyExpression as? KtReturnExpression)?.returnedExpression ?: bodyExpression
                }

                val typeInfo = TypeInfo(
                    originalElement.getExpressionForTypeGuess(),
                    if (varExpected) Variance.INVARIANT else Variance.OUT_VARIANCE
                )
                val propertyInfo = PropertyInfo(propertyName, TypeInfo.Empty, typeInfo, varExpected, Collections.singletonList(actualContainer))

                with(CallableBuilderConfiguration(listOfNotNull(propertyInfo), originalElement, file, editor).createBuilder()) {
                    placement = CallablePlacement.NoReceiver(actualContainer)
                    project.executeCommand(text) { build() }
                }
            }
        }
    }
}
