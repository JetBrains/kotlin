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
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.psi.JetForExpression
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.types.TypeProjectionImpl
import java.util.Collections
import org.jetbrains.kotlin.types.JetTypeImpl
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.kotlin.idea.quickfix.JetIntentionActionsFactory

object CreateIteratorFunctionActionFactory : JetIntentionActionsFactory() {
    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction>? {
        val file = diagnostic.getPsiFile() as? JetFile ?: return null
        val forExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetForExpression>()) ?: return null
        val iterableExpr = forExpr.getLoopRange() ?: return null
        val variableExpr: JetExpression = ((forExpr.getLoopParameter() ?: forExpr.getMultiParameter()) ?: return null) as JetExpression
        val iterableType = TypeInfo(iterableExpr, Variance.IN_VARIANCE)
        val returnJetType = KotlinBuiltIns.getInstance().getIterator().getDefaultType()

        val analysisResult = file.analyzeFullyAndGetResult()
        val returnJetTypeParameterTypes = variableExpr.guessTypes(analysisResult.bindingContext, analysisResult.moduleDescriptor)
        if (returnJetTypeParameterTypes.size() != 1) return null

        val returnJetTypeParameterType = TypeProjectionImpl(returnJetTypeParameterTypes[0])
        val returnJetTypeArguments = Collections.singletonList(returnJetTypeParameterType)
        val newReturnJetType = JetTypeImpl(returnJetType.getAnnotations(), returnJetType.getConstructor(), returnJetType.isMarkedNullable(), returnJetTypeArguments, returnJetType.getMemberScope())
        val returnType = TypeInfo(newReturnJetType, Variance.OUT_VARIANCE)
        return CreateCallableFromUsageFixes(forExpr, FunctionInfo("iterator", iterableType, returnType))
    }
}
