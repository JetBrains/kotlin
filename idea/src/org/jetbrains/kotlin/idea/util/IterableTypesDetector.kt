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

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.types.TypeUtils
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.di.InjectorForMacros
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.scopes.JetScope
import java.util.HashMap
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

public class IterableTypesDetector(
        private val project: Project,
        private val moduleDescriptor: ModuleDescriptor,
        private val scope: JetScope,
        private val loopVarType: JetType? = null
) {

    private val injector = InjectorForMacros(project, moduleDescriptor)
    private val cache = HashMap<JetType, Boolean>()
    private val iteratorName = Name.identifier("iterator")

    private val typesWithExtensionIterator: Collection<JetType> = scope.getFunctions(iteratorName)
            .map { it.getExtensionReceiverParameter() }
            .filterNotNull()
            .map { it.getType() }

    public fun isIterable(type: JetType): Boolean {
        return cache.getOrPut(type, { isIterableNoCache(type) })
    }

    private fun isIterableNoCache(type: JetType): Boolean {
        // optimization
        if (!canBeIterable(type)) return false

        val expression = JetPsiFactory(project).createExpression("fake")
        val expressionReceiver = ExpressionReceiver(expression, type)
        val context = ExpressionTypingContext.newContext(injector.getExpressionTypingServices(), BindingTraceContext(), scope, DataFlowInfo.EMPTY, TypeUtils.NO_EXPECTED_TYPE)
        val elementType = injector.getExpressionTypingComponents().getForLoopConventionsChecker().checkIterableConvention(expressionReceiver, context)
        if (elementType == null) return false
        return loopVarType == null || elementType.isSubtypeOf(loopVarType)
    }

    private fun canBeIterable(type: JetType): Boolean {
        return type.getMemberScope().getFunctions(iteratorName).isEmpty() || typesWithExtensionIterator.any { type.isSubtypeOf(type) }
    }
}