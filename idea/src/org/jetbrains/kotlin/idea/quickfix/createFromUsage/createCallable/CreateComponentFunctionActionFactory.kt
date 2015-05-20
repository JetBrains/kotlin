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
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.resolve.dataClassUtils.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.psi.JetMultiDeclaration
import org.jetbrains.kotlin.psi.JetForExpression
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.kotlin.idea.quickfix.JetIntentionActionsFactory

object CreateComponentFunctionActionFactory : JetIntentionActionsFactory() {
    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction>? {
        val diagnosticWithParameters = Errors.COMPONENT_FUNCTION_MISSING.cast(diagnostic)
        val name = diagnosticWithParameters.getA()
        if (!isComponentLike(name)) return null

        val componentNumber = getComponentIndex(name) - 1

        var multiDeclaration = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetMultiDeclaration>())
        val ownerType = if (multiDeclaration == null) {
            val forExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetForExpression>())!!
            multiDeclaration = forExpr.getMultiParameter()!!
            TypeInfo(diagnosticWithParameters.getB(), Variance.IN_VARIANCE)
        }
        else {
            val rhs = multiDeclaration.getInitializer() ?: return null
            TypeInfo(rhs, Variance.IN_VARIANCE)
        }
        val entries = multiDeclaration.getEntries()

        val entry = entries[componentNumber]
        val returnType = TypeInfo(entry, Variance.OUT_VARIANCE)

        return CreateCallableFromUsageFixes(multiDeclaration, FunctionInfo(name.getIdentifier(), ownerType, returnType))
    }
}
