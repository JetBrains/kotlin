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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.FunctionInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions

object CreateNextFunctionActionFactory : CreateCallableMemberFromUsageFactory<KtForExpression>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtForExpression? {
        return QuickFixUtil.getParentElementOfType(diagnostic, KtForExpression::class.java)
    }

    override fun createCallableInfo(element: KtForExpression, diagnostic: Diagnostic): CallableInfo? {
        val diagnosticWithParameters = DiagnosticFactory.cast(diagnostic, Errors.NEXT_MISSING, Errors.NEXT_NONE_APPLICABLE)
        val ownerType = TypeInfo(diagnosticWithParameters.a, Variance.IN_VARIANCE)

        val variableExpr = element.loopParameter ?: element.destructuringDeclaration ?: return null
        val returnType = TypeInfo(variableExpr as KtExpression, Variance.OUT_VARIANCE)
        return FunctionInfo(OperatorNameConventions.NEXT.asString(), ownerType, returnType, isOperator = true)
    }
}
