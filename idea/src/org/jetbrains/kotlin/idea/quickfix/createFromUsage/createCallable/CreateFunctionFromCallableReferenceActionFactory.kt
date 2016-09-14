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

import com.intellij.util.SmartList
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.kotlin.idea.refactoring.getExtractionContainers
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS
import org.jetbrains.kotlin.utils.ifEmpty

object CreateFunctionFromCallableReferenceActionFactory : CreateCallableMemberFromUsageFactory<KtCallableReferenceExpression>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtCallableReferenceExpression? {
        return diagnostic.psiElement.getStrictParentOfType<KtCallableReferenceExpression>()
    }

    override fun extractFixData(element: KtCallableReferenceExpression, diagnostic: Diagnostic): List<CallableInfo> {
        val name = element.callableReference.getReferencedName()
        val resolutionFacade = element.getResolutionFacade()
        val context = resolutionFacade.analyze(element, BodyResolveMode.PARTIAL)
        return element
                .guessTypes(context, resolutionFacade.moduleDescriptor)
                .filter(KotlinType::isFunctionType)
                .mapNotNull {
                    val expectedReceiverType = it.getReceiverTypeFromFunctionType()
                    val receiverExpression = element.receiverExpression
                    val qualifierType = (context.get(BindingContext.DOUBLE_COLON_LHS, receiverExpression) as? DoubleColonLHS.Type)?.type
                    val receiverTypeInfo = qualifierType?.let { TypeInfo(it, Variance.IN_VARIANCE) } ?: TypeInfo.Empty
                    val returnTypeInfo = TypeInfo(it.getReturnTypeFromFunctionType(), Variance.OUT_VARIANCE)
                    val containers = element.getExtractionContainers(includeAll = true).ifEmpty { return@mapNotNull null }
                    val parameterInfos = SmartList<ParameterInfo>().apply {
                        if (receiverExpression == null && expectedReceiverType != null) {
                            add(ParameterInfo(TypeInfo(expectedReceiverType, Variance.IN_VARIANCE)))
                        }

                        it.getValueParameterTypesFromFunctionType()
                                .let {
                                    if (receiverExpression != null && expectedReceiverType == null && it.isNotEmpty())
                                        it.subList(1, it.size)
                                    else it
                                }
                                .mapTo(this) {
                                    ParameterInfo(TypeInfo(it.type, it.projectionKind))
                                }
                    }

                    FunctionInfo(name, receiverTypeInfo, returnTypeInfo, containers, parameterInfos)
                }
    }
}
