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
import org.jetbrains.kotlin.idea.util.FuzzyType
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.scopes.KtScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.utils.asLexicalScope
import org.jetbrains.kotlin.types.KtType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.expressions.ForLoopConventionsChecker
import java.util.HashMap

public class IterableTypesDetection(
        private val project: Project,
        private val moduleDescriptor: ModuleDescriptor,
        private val forLoopConventionsChecker: ForLoopConventionsChecker
) {
    companion object {
        private val iteratorName = Name.identifier("iterator")
    }

    public fun createDetector(scope: KtScope): IterableTypesDetector {
        return Detector(scope)
    }
    private inner class Detector(private val scope: KtScope): IterableTypesDetector {
        private val cache = HashMap<FuzzyType, FuzzyType?>()

        private val typesWithExtensionIterator: Collection<KtType> = scope.getFunctions(iteratorName, NoLookupLocation.FROM_IDE)
                .map { it.getExtensionReceiverParameter() }
                .filterNotNull()
                .map { it.getType() }

        override fun isIterable(type: FuzzyType, loopVarType: KtType?): Boolean {
            val elementType = elementType(type) ?: return false
            return loopVarType == null || elementType.checkIsSubtypeOf(loopVarType) != null
        }

        override fun isIterable(type: KtType, loopVarType: KtType?): Boolean
                = isIterable(FuzzyType(type, emptyList()), loopVarType)

        private fun elementType(type: FuzzyType): FuzzyType? {
            return cache.getOrPut(type, { elementTypeNoCache(type) })
        }

        override fun elementType(type: KtType): FuzzyType?
                = elementType(FuzzyType(type, emptyList()))

        private fun elementTypeNoCache(type: FuzzyType): FuzzyType? {
            // optimization
            if (!canBeIterable(type)) return null

            val expression = KtPsiFactory(project).createExpression("fake")
            val expressionReceiver = ExpressionReceiver(expression, type.type)
            val context = ExpressionTypingContext.newContext(BindingTraceContext(), scope.asLexicalScope(), DataFlowInfo.EMPTY, TypeUtils.NO_EXPECTED_TYPE)
            val elementType = forLoopConventionsChecker.checkIterableConvention(expressionReceiver, context)
            return elementType?.let { FuzzyType(it, type.freeParameters) }
        }

        private fun canBeIterable(type: FuzzyType): Boolean {
            return type.type.memberScope.getFunctions(iteratorName, NoLookupLocation.FROM_IDE).isNotEmpty() ||
                   typesWithExtensionIterator.any { type.checkIsSubtypeOf(it) != null }
        }
    }
}

public interface IterableTypesDetector {
    public fun isIterable(type: KtType, loopVarType: KtType? = null): Boolean

    public fun isIterable(type: FuzzyType, loopVarType: KtType? = null): Boolean

    public fun elementType(type: KtType): FuzzyType?
}