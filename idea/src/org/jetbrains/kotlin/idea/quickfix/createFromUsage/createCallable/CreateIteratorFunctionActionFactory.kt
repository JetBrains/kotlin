/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.FunctionInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.guessTypes
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.*

object CreateIteratorFunctionActionFactory : CreateCallableMemberFromUsageFactory<KtForExpression>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtForExpression? {
        return QuickFixUtil.getParentElementOfType(diagnostic, KtForExpression::class.java)
    }

    override fun createCallableInfo(element: KtForExpression, diagnostic: Diagnostic): CallableInfo? {
        val file = diagnostic.psiFile as? KtFile ?: return null
        val iterableExpr = element.loopRange ?: return null
        val variableExpr: KtExpression = ((element.loopParameter ?: element.destructuringDeclaration) ?: return null) as KtExpression
        val iterableType = TypeInfo(iterableExpr, Variance.IN_VARIANCE)

        val (bindingContext, moduleDescriptor) = file.analyzeFullyAndGetResult()

        val returnJetType = moduleDescriptor.builtIns.iterator.defaultType
        val returnJetTypeParameterTypes = variableExpr.guessTypes(bindingContext, moduleDescriptor)
        if (returnJetTypeParameterTypes.size != 1) return null

        val returnJetTypeParameterType = TypeProjectionImpl(returnJetTypeParameterTypes[0])
        val returnJetTypeArguments = Collections.singletonList(returnJetTypeParameterType)
        val newReturnJetType = KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(returnJetType.annotations,
                                                            returnJetType.constructor,
                                                            returnJetTypeArguments,
                                                            returnJetType.isMarkedNullable,
                                                            returnJetType.memberScope)
        val returnType = TypeInfo(newReturnJetType, Variance.OUT_VARIANCE)
        return FunctionInfo(OperatorNameConventions.ITERATOR.asString(), iterableType, returnType, isOperator = true)
    }
}
