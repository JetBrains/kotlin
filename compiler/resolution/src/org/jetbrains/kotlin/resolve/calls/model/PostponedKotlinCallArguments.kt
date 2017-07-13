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

package org.jetbrains.kotlin.resolve.calls.model

import org.jetbrains.kotlin.builtins.createFunctionType
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.calls.components.CallableReferenceCandidate
import org.jetbrains.kotlin.resolve.calls.components.getFunctionTypeFromCallableReferenceExpectedType
import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.typeUtil.builtIns


sealed class PostponedKotlinCallArgument {
    abstract val argument: PostponableKotlinCallArgument

    abstract val analyzed: Boolean

    abstract val inputTypes: Collection<UnwrappedType>
    abstract val outputType: UnwrappedType?
}

class PostponedLambdaArgument(
        override val argument: LambdaKotlinCallArgument,
        val isSuspend: Boolean,
        val receiver: UnwrappedType?,
        val parameters: List<UnwrappedType>,
        val returnType: UnwrappedType
) : PostponedKotlinCallArgument() {
    override var analyzed: Boolean = false

    val type: SimpleType = createFunctionType(returnType.builtIns, Annotations.EMPTY, receiver, parameters, null, returnType, isSuspend) // todo support annotations

    override val inputTypes: Collection<UnwrappedType> get() = receiver?.let { parameters + it } ?: parameters
    override val outputType: UnwrappedType get() = returnType

    lateinit var resultArguments: List<SimpleKotlinCallArgument>
    lateinit var finalReturnType: UnwrappedType
}

class PostponedCallableReferenceArgument(
        override val argument: CallableReferenceKotlinCallArgument,
        val expectedType: UnwrappedType
) : PostponedKotlinCallArgument() {
    override var analyzed: Boolean = false

    override val inputTypes: Collection<UnwrappedType>
        get() {
            val functionType = getFunctionTypeFromCallableReferenceExpectedType(expectedType) ?: return emptyList()
            val parameters = functionType.getValueParameterTypesFromFunctionType().map { it.type.unwrap() }
            val receiver = functionType.getReceiverTypeFromFunctionType()?.unwrap()
            return receiver?.let { parameters + it } ?: parameters
        }

    override val outputType: UnwrappedType?
        get() {
            val functionType = getFunctionTypeFromCallableReferenceExpectedType(expectedType) ?: return null
            return functionType.getReturnTypeFromFunctionType().unwrap()
        }

    var analyzedAndThereIsResult: Boolean = false

    lateinit var myTypeVariables: List<NewTypeVariable>
    lateinit var callableResolutionCandidate: CallableReferenceCandidate
}

class PostponedCollectionLiteralArgument(
        override val argument: CollectionLiteralKotlinCallArgument,
        val expectedType: UnwrappedType
) : PostponedKotlinCallArgument() {
    // for now we consider all such arguments as analyzed because they processed via special logic anyway
    override val analyzed get() = true

    override val inputTypes: Collection<UnwrappedType>
        get() = emptyList()
    override val outputType: UnwrappedType?
        get() = null
}