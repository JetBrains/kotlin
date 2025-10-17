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

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.ContractsDslNames.CALLS_IN_PLACE
import org.jetbrains.kotlin.resolve.ContractsDslNames.CONTRACT
import org.jetbrains.kotlin.resolve.ContractsDslNames.CONTRACTS_DSL_ANNOTATION_FQN
import org.jetbrains.kotlin.resolve.ContractsDslNames.EFFECT
import org.jetbrains.kotlin.resolve.ContractsDslNames.IMPLIES
import org.jetbrains.kotlin.resolve.ContractsDslNames.INVOCATION_KIND_ENUM
import org.jetbrains.kotlin.resolve.ContractsDslNames.RETURNS
import org.jetbrains.kotlin.resolve.ContractsDslNames.RETURNS_NOT_NULL
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.typeUtil.isBoolean
import org.jetbrains.kotlin.types.typeUtil.isNullableAny


fun DeclarationDescriptor.isFromContractDsl(): Boolean = this.annotations.hasAnnotation(CONTRACTS_DSL_ANNOTATION_FQN)

fun DeclarationDescriptor.isContractCallDescriptor(): Boolean = equalsDslDescriptor(CONTRACT.callableName)

fun DeclarationDescriptor.isImpliesCallDescriptor(): Boolean = equalsDslDescriptor(IMPLIES.callableName)

fun DeclarationDescriptor.isReturnsEffectDescriptor(): Boolean = equalsDslDescriptor(RETURNS.callableName)

fun DeclarationDescriptor.isReturnsNotNullDescriptor(): Boolean = equalsDslDescriptor(RETURNS_NOT_NULL.callableName)

fun DeclarationDescriptor.isReturnsWildcardDescriptor(): Boolean = equalsDslDescriptor(RETURNS.callableName) &&
        this is FunctionDescriptor &&
        valueParameters.isEmpty()

fun DeclarationDescriptor.isEffectDescriptor(): Boolean = equalsDslDescriptor(EFFECT.callableName)

fun DeclarationDescriptor.isCallsInPlaceEffectDescriptor(): Boolean = equalsDslDescriptor(CALLS_IN_PLACE.callableName)

fun DeclarationDescriptor.isInvocationKindEnum(): Boolean = equalsDslDescriptor(INVOCATION_KIND_ENUM.callableName)

fun DeclarationDescriptor.isEqualsDescriptor(): Boolean =
    this is FunctionDescriptor && this.name == Name.identifier("equals") && dispatchReceiverParameter != null && // fast checks
            this.returnType?.isBoolean() == true && this.valueParameters.singleOrNull()?.type?.isNullableAny() == true // signature matches

internal fun ResolvedCall<*>.firstArgumentAsExpressionOrNull(): KtExpression? =
    (this.valueArgumentsByIndex?.firstOrNull() as? ExpressionValueArgument)?.valueArgument?.getArgumentExpression()

private fun DeclarationDescriptor.equalsDslDescriptor(dslName: Name): Boolean = this.name == dslName && this.isFromContractDsl()
