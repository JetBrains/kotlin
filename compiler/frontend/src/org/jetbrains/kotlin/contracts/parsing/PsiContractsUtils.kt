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

package org.jetbrains.kotlin.contracts.parsing

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.contracts.parsing.ContractsDslNames.CALLS_IN_PLACE
import org.jetbrains.kotlin.contracts.parsing.ContractsDslNames.CONTRACT
import org.jetbrains.kotlin.contracts.parsing.ContractsDslNames.CONTRACTS_DSL_ANNOTATION_FQN
import org.jetbrains.kotlin.contracts.parsing.ContractsDslNames.EFFECT
import org.jetbrains.kotlin.contracts.parsing.ContractsDslNames.IMPLIES
import org.jetbrains.kotlin.contracts.parsing.ContractsDslNames.INVOCATION_KIND_ENUM
import org.jetbrains.kotlin.contracts.parsing.ContractsDslNames.RETURNS
import org.jetbrains.kotlin.contracts.parsing.ContractsDslNames.RETURNS_NOT_NULL
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.typeUtil.isBoolean
import org.jetbrains.kotlin.types.typeUtil.isNullableAny
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


object ContractsDslNames {
    // Internal marker-annotation for distinguishing our API
    val CONTRACTS_DSL_ANNOTATION_FQN = FqName("kotlin.internal.ContractsDsl")

    // Types
    val EFFECT = Name.identifier("Effect")
    val CONDITIONAL_EFFECT = Name.identifier("ConditionalEffect")
    val SIMPLE_EFFECT = Name.identifier("SimpleEffect")
    val RETURNS_EFFECT = Name.identifier("Returns")
    val RETURNS_NOT_NULL_EFFECT = Name.identifier("ReturnsNotNull")
    val CALLS_IN_PLACE_EFFECT = Name.identifier("CallsInPlace")

    // Structure-defining calls
    val CONTRACT = Name.identifier("contract")
    val IMPLIES = Name.identifier("implies")

    // Effect-declaration calls
    val RETURNS = Name.identifier("returns")
    val RETURNS_NOT_NULL = Name.identifier("returnsNotNull")
    val CALLS_IN_PLACE = Name.identifier("callsInPlace")

    // enum class InvocationKind
    val INVOCATION_KIND_ENUM = Name.identifier("InvocationKind")
    val EXACTLY_ONCE_KIND = Name.identifier("EXACTLY_ONCE")
    val AT_LEAST_ONCE_KIND = Name.identifier("AT_LEAST_ONCE")
    val UNKNOWN_KIND = Name.identifier("UNKNOWN")
    val AT_MOST_ONCE_KIND = Name.identifier("AT_MOST_ONCE")
}

fun DeclarationDescriptor.isFromContractDsl(): Boolean = this.annotations.hasAnnotation(CONTRACTS_DSL_ANNOTATION_FQN)

fun DeclarationDescriptor.isContractCallDescriptor(): Boolean = equalsDslDescriptor(CONTRACT)

fun DeclarationDescriptor.isImpliesCallDescriptor(): Boolean = equalsDslDescriptor(IMPLIES)

fun DeclarationDescriptor.isReturnsEffectDescriptor(): Boolean = equalsDslDescriptor(RETURNS)

fun DeclarationDescriptor.isReturnsNotNullDescriptor(): Boolean = equalsDslDescriptor(RETURNS_NOT_NULL)

fun DeclarationDescriptor.isReturnsWildcardDescriptor(): Boolean = equalsDslDescriptor(RETURNS) &&
        this is FunctionDescriptor &&
        valueParameters.isEmpty()

fun DeclarationDescriptor.isEffectDescriptor(): Boolean = equalsDslDescriptor(EFFECT)

fun DeclarationDescriptor.isCallsInPlaceEffectDescriptor(): Boolean = equalsDslDescriptor(CALLS_IN_PLACE)

fun DeclarationDescriptor.isInvocationKindEnum(): Boolean = equalsDslDescriptor(INVOCATION_KIND_ENUM)

fun DeclarationDescriptor.isEqualsDescriptor(): Boolean =
    this is FunctionDescriptor && this.name == Name.identifier("equals") && dispatchReceiverParameter != null && // fast checks
            this.returnType?.isBoolean() == true && this.valueParameters.singleOrNull()?.type?.isNullableAny() == true // signature matches

internal fun ResolvedCall<*>.firstArgumentAsExpressionOrNull(): KtExpression? =
    this.valueArgumentsByIndex?.firstOrNull()?.safeAs<ExpressionValueArgument>()?.valueArgument?.getArgumentExpression()

private fun DeclarationDescriptor.equalsDslDescriptor(dslName: Name): Boolean = this.name == dslName && this.isFromContractDsl()