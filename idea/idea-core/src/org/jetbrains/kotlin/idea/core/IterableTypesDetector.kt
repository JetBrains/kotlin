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

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.frontend.di.createContainerForMacros
import org.jetbrains.kotlin.idea.util.FuzzyType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import java.util.HashMap

public class IterableTypesDetector(
        private val project: Project,
        private val moduleDescriptor: ModuleDescriptor,
        private val scope: JetScope
) {

    private val container = createContainerForMacros(project, moduleDescriptor)
    private val cache = HashMap<FuzzyType, FuzzyType?>()
    private val iteratorName = Name.identifier("iterator")

    private val typesWithExtensionIterator: Collection<JetType> = scope.getFunctions(iteratorName)
            .map { it.getExtensionReceiverParameter() }
            .filterNotNull()
            .map { it.getType() }

    public fun isIterable(type: FuzzyType, loopVarType: JetType? = null): Boolean {
        val elementType = elementType(type) ?: return false
        return loopVarType == null || elementType.checkIsSubtypeOf(loopVarType) != null
    }

    public fun isIterable(type: JetType, loopVarType: JetType? = null): Boolean
            = isIterable(FuzzyType(type, emptyList()), loopVarType)

    public fun elementType(type: FuzzyType): FuzzyType? {
        return cache.getOrPut(type, { elementTypeNoCache(type) })
    }

    public fun elementType(type: JetType): FuzzyType?
            = elementType(FuzzyType(type, emptyList()))

    private fun elementTypeNoCache(type: FuzzyType): FuzzyType? {
        // optimization
        if (!canBeIterable(type)) return null

        val expression = JetPsiFactory(project).createExpression("fake")
        val expressionReceiver = ExpressionReceiver(expression, type.type)
        val expressionTypingComponents = container.expressionTypingComponents
        val context = ExpressionTypingContext.newContext(BindingTraceContext(), scope, DataFlowInfo.EMPTY, TypeUtils.NO_EXPECTED_TYPE)
        val elementType = expressionTypingComponents.getForLoopConventionsChecker().checkIterableConvention(expressionReceiver, context)
        return elementType?.let { FuzzyType(it, type.freeParameters) }
    }

    private fun canBeIterable(type: FuzzyType): Boolean {
        return type.type.getMemberScope().getFunctions(iteratorName).isNotEmpty() || typesWithExtensionIterator.any { type.checkIsSubtypeOf(it) != null }
    }
}