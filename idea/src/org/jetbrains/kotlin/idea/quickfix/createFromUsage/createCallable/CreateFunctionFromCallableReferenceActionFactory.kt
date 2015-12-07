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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.refactoring.getExtractionContainers
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.Variance
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
                .filter { KotlinBuiltIns.isExactFunctionType(it) || KotlinBuiltIns.isExactExtensionFunctionType(it) }
                .mapNotNull {
                    val receiverTypeInfo = element.typeReference?.let { TypeInfo(it, Variance.IN_VARIANCE) } ?: TypeInfo.Empty
                    val returnTypeInfo = TypeInfo(KotlinBuiltIns.getReturnTypeFromFunctionType(it), Variance.OUT_VARIANCE)
                    val containers = element.getExtractionContainers(includeAll = true).ifEmpty { return@mapNotNull  null }
                    val parameterInfos = SmartList<ParameterInfo>().apply {
                        if (element.typeReference == null) {
                            KotlinBuiltIns.getReceiverType(it)?.let { add(ParameterInfo(TypeInfo(it, Variance.IN_VARIANCE))) }
                        }
                        KotlinBuiltIns
                                .getParameterTypeProjectionsFromFunctionType(it)
                                .mapTo(this) { ParameterInfo(TypeInfo(it.type, it.projectionKind)) }
                    }

                    FunctionInfo(name, receiverTypeInfo, returnTypeInfo, containers, parameterInfos)
                }
    }
}