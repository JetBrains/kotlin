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

package org.jetbrains.kotlin.idea.intentions.loopToCallChain

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.nullability

interface FindOperatorGenerator {
    val functionName: String

    val shouldUseInputVariable: Boolean

    fun generate(chainedCallGenerator: ChainedCallGenerator, filter: KtExpression?): KtExpression

    val chainCallCount: Int
        get() = 1
}

fun buildFindOperationGenerator(
        valueIfFound: KtExpression,
        valueIfNotFound: KtExpression,
        inputVariable: KtCallableDeclaration,
        findFirst: Boolean
): FindOperatorGenerator?  {
    assert(valueIfFound.isPhysical)
    assert(valueIfNotFound.isPhysical)

    fun generateChainedCall(stdlibFunName: String, chainedCallGenerator: ChainedCallGenerator, filter: KtExpression?): KtExpression {
        return if (filter == null) {
            chainedCallGenerator.generate("$stdlibFunName()")
        }
        else {
            val lambda = generateLambda(inputVariable, filter)
            chainedCallGenerator.generate("$stdlibFunName $0:'{}'", lambda)
        }
    }

    class SimpleGenerator(override val functionName: String, override val shouldUseInputVariable: Boolean) : FindOperatorGenerator {
        override fun generate(chainedCallGenerator: ChainedCallGenerator, filter: KtExpression?): KtExpression {
            return generateChainedCall(functionName, chainedCallGenerator, filter)
        }
    }

    val inputVariableCanHoldNull = (inputVariable.resolveToDescriptor() as VariableDescriptor).type.nullability() != TypeNullability.NOT_NULL

    fun FindOperatorGenerator.useElvisOperatorIfNeeded(): FindOperatorGenerator? {
        if (valueIfNotFound.isNullExpression()) return this

        // we cannot use ?: if found value can be null
        if (inputVariableCanHoldNull) return null

        return object: FindOperatorGenerator by this {
            override fun generate(chainedCallGenerator: ChainedCallGenerator, filter: KtExpression?): KtExpression {
                val generated = this@useElvisOperatorIfNeeded.generate(chainedCallGenerator, filter)
                return KtPsiFactory(generated).createExpressionByPattern("$0 ?: $1", generated, valueIfNotFound)
            }
        }
    }

    when {
        valueIfFound.isVariableReference(inputVariable) -> {
            val generator = SimpleGenerator(if (findFirst) "firstOrNull" else "lastOrNull", shouldUseInputVariable = true)
            return generator.useElvisOperatorIfNeeded()
        }

        valueIfFound.isTrueConstant() && valueIfNotFound.isFalseConstant() -> return SimpleGenerator("any", shouldUseInputVariable = false)

        valueIfFound.isFalseConstant() && valueIfNotFound.isTrueConstant() -> return SimpleGenerator("none", shouldUseInputVariable = false)

        inputVariable.hasUsages(valueIfFound) -> {
            if (!findFirst) return null // too dangerous because of side effects

            // specially handle the case when the result expression is "<input variable>.<some call>" or "<input variable>?.<some call>"
            val qualifiedExpression = valueIfFound as? KtQualifiedExpression
            if (qualifiedExpression != null) {
                val receiver = qualifiedExpression.receiverExpression
                val selector = qualifiedExpression.selectorExpression
                if (receiver.isVariableReference(inputVariable) && selector != null && !inputVariable.hasUsages(selector)) {
                    return object: FindOperatorGenerator {
                        override val functionName: String
                            get() = "firstOrNull"

                        override val shouldUseInputVariable: Boolean
                            get() = true

                        override val chainCallCount: Int
                            get() = 2

                        override fun generate(chainedCallGenerator: ChainedCallGenerator, filter: KtExpression?): KtExpression {
                            val findFirstCall = generateChainedCall(functionName, chainedCallGenerator, filter)
                            return chainedCallGenerator.generate("$0", selector, receiver = findFirstCall, safeCall = true)
                        }
                    }.useElvisOperatorIfNeeded()
                }
            }

            // in case of nullable input variable we cannot distinguish by the result of "firstOrNull" whether nothing was found or 'null' was found
            if (inputVariableCanHoldNull) return null

            return object: FindOperatorGenerator {
                override val functionName: String
                    get() = "firstOrNull"

                override val shouldUseInputVariable: Boolean
                    get() = true

                override val chainCallCount: Int
                    get() = 2 // also includes "let"

                override fun generate(chainedCallGenerator: ChainedCallGenerator, filter: KtExpression?): KtExpression {
                    val findFirstCall = generateChainedCall(functionName, chainedCallGenerator, filter)
                    val letBody = generateLambda(inputVariable, valueIfFound)
                    return chainedCallGenerator.generate("let $0:'{}'", letBody, receiver = findFirstCall, safeCall = true)
                }
            }.useElvisOperatorIfNeeded()
        }

        else -> {
            return object: FindOperatorGenerator {
                override val functionName: String
                    get() = "any"

                override val shouldUseInputVariable: Boolean
                    get() = false

                override fun generate(chainedCallGenerator: ChainedCallGenerator, filter: KtExpression?): KtExpression {
                    val chainedCall = generateChainedCall(functionName, chainedCallGenerator, filter)
                    return KtPsiFactory(chainedCall).createExpressionByPattern("if ($0) $1 else $2", chainedCall, valueIfFound, valueIfNotFound)
                }
            }
        }
    }
}
