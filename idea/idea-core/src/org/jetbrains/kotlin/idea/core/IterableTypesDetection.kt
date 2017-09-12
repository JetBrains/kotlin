/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.util.FuzzyType
import org.jetbrains.kotlin.idea.util.toFuzzyType
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.utils.collectFunctions
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.expressions.ForLoopConventionsChecker
import org.jetbrains.kotlin.util.isValidOperator
import org.jetbrains.kotlin.utils.getOrPutNullable
import java.util.*

class IterableTypesDetection(
        private val project: Project,
        private val forLoopConventionsChecker: ForLoopConventionsChecker
) {
    companion object {
        private val iteratorName = Name.identifier("iterator")
    }

    fun createDetector(scope: LexicalScope): IterableTypesDetector {
        return Detector(scope)
    }
    private inner class Detector(private val scope: LexicalScope): IterableTypesDetector {
        private val cache = HashMap<FuzzyType, FuzzyType?>()

        private val typesWithExtensionIterator: Collection<KotlinType> = scope
                .collectFunctions(iteratorName, NoLookupLocation.FROM_IDE)
                .filter { it.isValidOperator() }
                .mapNotNull { it.extensionReceiverParameter?.type }

        override fun isIterable(type: FuzzyType, loopVarType: KotlinType?): Boolean {
            val elementType = elementType(type) ?: return false
            return loopVarType == null || elementType.checkIsSubtypeOf(loopVarType) != null
        }

        override fun isIterable(type: KotlinType, loopVarType: KotlinType?): Boolean
                = isIterable(type.toFuzzyType(emptyList()), loopVarType)

        private fun elementType(type: FuzzyType): FuzzyType? {
            return cache.getOrPutNullable(type, { elementTypeNoCache(type) })
        }

        override fun elementType(type: KotlinType): FuzzyType?
                = elementType(type.toFuzzyType(emptyList()))

        private fun elementTypeNoCache(type: FuzzyType): FuzzyType? {
            // optimization
            if (!canBeIterable(type)) return null

            val expression = KtPsiFactory(project).createExpression("fake")
            val context = ExpressionTypingContext.newContext(
                    BindingTraceContext(), scope, DataFlowInfo.EMPTY, TypeUtils.NO_EXPECTED_TYPE, expression.languageVersionSettings)
            val expressionReceiver = ExpressionReceiver.create(expression, type.type, context.trace.bindingContext)
            val elementType = forLoopConventionsChecker.checkIterableConvention(expressionReceiver, context)
            return elementType?.let { it.toFuzzyType(type.freeParameters) }
        }

        private fun canBeIterable(type: FuzzyType): Boolean {
            return type.type.memberScope.getContributedFunctions(iteratorName, NoLookupLocation.FROM_IDE).isNotEmpty() ||
                   typesWithExtensionIterator.any {
                       val freeParams = it.arguments.mapNotNull { it.type.constructor.declarationDescriptor as? TypeParameterDescriptor }
                       type.checkIsSubtypeOf(it.toFuzzyType(freeParams)) != null
                   }
        }
    }
}

interface IterableTypesDetector {
    fun isIterable(type: KotlinType, loopVarType: KotlinType? = null): Boolean

    fun isIterable(type: FuzzyType, loopVarType: KotlinType? = null): Boolean

    fun elementType(type: KotlinType): FuzzyType?
}