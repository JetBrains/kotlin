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

package org.jetbrains.kotlin.idea.core

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.getImplicitReceiversWithInstanceToExpression
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver
import java.util.HashMap

public fun Call.mapArgumentsToParameters(targetDescriptor: CallableDescriptor): Map<ValueArgument, ValueParameterDescriptor> {
    val parameters = targetDescriptor.getValueParameters()
    if (parameters.isEmpty()) return emptyMap()

    val map = HashMap<ValueArgument, ValueParameterDescriptor>()
    val parametersByName = parameters.toMap { it.getName() }

    var positionalArgumentIndex: Int? = 0

    for (argument in getValueArguments()) {
        if (argument is FunctionLiteralArgument) {
            map[argument] = parameters.last()
        }
        else {
            val argumentName = argument.getArgumentName()?.asName

            if (argumentName != null) {
                if (targetDescriptor.hasStableParameterNames()) {
                    val parameter = parametersByName[argumentName]
                    if (parameter != null) {
                        map[argument] = parameter
                    }
                }
                positionalArgumentIndex = null
            }
            else {
                if (positionalArgumentIndex != null && positionalArgumentIndex < parameters.size()) {
                    val parameter = parameters[positionalArgumentIndex]
                    map[argument] = parameter

                    if (parameter.getVarargElementType() == null) {
                        positionalArgumentIndex++
                    }
                }
            }
        }
    }

    return map
}

public fun ThisReceiver.asExpression(resolutionScope: JetScope, psiFactory: JetPsiFactory): JetExpression? {
    val expressionFactory = resolutionScope.getImplicitReceiversWithInstanceToExpression()
                                    .entrySet()
                                    .firstOrNull { it.key.getContainingDeclaration() == this.getDeclarationDescriptor() }
                                    ?.value ?: return null
    return expressionFactory.createExpression(psiFactory)
}

public fun PsiElement.getResolutionScope(bindingContext: BindingContext, resolutionFacade: ResolutionFacade): JetScope {
    for (parent in parentsWithSelf) {
        if (parent is JetExpression) {
            val scope = bindingContext[BindingContext.RESOLUTION_SCOPE, parent]
            if (scope != null) return scope
        }

        if (parent is JetClassBody) {
            val classDescriptor = bindingContext[BindingContext.CLASS, parent.getParent()] as? ClassDescriptorWithResolutionScopes
            if (classDescriptor != null) {
                return classDescriptor.getScopeForMemberDeclarationResolution()
            }
        }

        if (parent is JetFile) {
            return resolutionFacade.getFileTopLevelScope(parent)
        }
    }
    error("Not in JetFile")
}
