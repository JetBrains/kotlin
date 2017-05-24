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

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.UnwrappedType

interface IsDescriptorFromSourcePredicate: (CallableDescriptor) -> Boolean

// This components hold state (trace). Work with this carefully.
interface KotlinResolutionCallbacks {
    fun analyzeAndGetLambdaResultArguments(
            topLevelCall: KotlinCall,
            lambdaArgument: LambdaKotlinCallArgument,
            isSuspend: Boolean,
            receiverType: UnwrappedType?,
            parameters: List<UnwrappedType>,
            expectedReturnType: UnwrappedType? // null means, that return type is not proper i.e. it depends on some type variables
    ): List<KotlinCallArgument>

    // todo this is hack for some client which try to read ResolvedCall from trace before all calls completed
    fun bindStubResolvedCallForCandidate(candidate: KotlinResolutionCandidate)

    fun completeCallableReference(callableReferenceArgument: ResolvedCallableReferenceArgument,
                                  resultTypeParameters: List<UnwrappedType>)

}