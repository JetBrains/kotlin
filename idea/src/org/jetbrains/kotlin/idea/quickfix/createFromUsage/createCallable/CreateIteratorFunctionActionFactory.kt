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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.FunctionInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.guessTypes
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetForExpression
import org.jetbrains.kotlin.types.JetTypeImpl
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.Variance
import java.util.Collections

object CreateIteratorFunctionActionFactory : CreateCallableMemberFromUsageFactory<JetForExpression>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): JetForExpression? {
        return QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetForExpression>())
    }

    override fun createCallableInfo(element: JetForExpression, diagnostic: Diagnostic): CallableInfo? {
        val file = diagnostic.psiFile as? JetFile ?: return null
        val iterableExpr = element.loopRange ?: return null
        val variableExpr: JetExpression = ((element.loopParameter ?: element.multiParameter) ?: return null) as JetExpression
        val iterableType = TypeInfo(iterableExpr, Variance.IN_VARIANCE)
        val returnJetType = KotlinBuiltIns.getInstance().iterator.defaultType

        val analysisResult = file.analyzeFullyAndGetResult()
        val returnJetTypeParameterTypes = variableExpr.guessTypes(analysisResult.bindingContext, analysisResult.moduleDescriptor)
        if (returnJetTypeParameterTypes.size() != 1) return null

        val returnJetTypeParameterType = TypeProjectionImpl(returnJetTypeParameterTypes[0])
        val returnJetTypeArguments = Collections.singletonList(returnJetTypeParameterType)
        val newReturnJetType = JetTypeImpl.create(returnJetType.annotations,
                                           returnJetType.constructor,
                                           returnJetType.isMarkedNullable,
                                           returnJetTypeArguments,
                                           returnJetType.memberScope)
        val returnType = TypeInfo(newReturnJetType, Variance.OUT_VARIANCE)
        return FunctionInfo("iterator", iterableType, returnType)
    }
}
