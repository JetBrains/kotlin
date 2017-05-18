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

package org.jetbrains.kotlin.resolve.calls.model

import org.jetbrains.kotlin.builtins.createFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.calls.components.CallableReferenceCandidate
import org.jetbrains.kotlin.resolve.calls.inference.model.LambdaTypeVariable
import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.typeUtil.builtIns


sealed class ArgumentWithPostponeResolution {
    abstract val outerCall: KotlinCall
    abstract val argument: KotlinCallArgument
    abstract val myTypeVariables: Collection<NewTypeVariable>
    abstract val inputType: Collection<UnwrappedType> // parameters and implicit receiver
    abstract val outputType: UnwrappedType?

    var analyzed: Boolean = false
}

class ResolvedLambdaArgument(
        override val outerCall: KotlinCall,
        override val argument: LambdaKotlinCallArgument,
        override val myTypeVariables: Collection<LambdaTypeVariable>,
        val isSuspend: Boolean,
        val receiver: UnwrappedType?,
        val parameters: List<UnwrappedType>,
        val returnType: UnwrappedType
) : ArgumentWithPostponeResolution() {
    val type: SimpleType = createFunctionType(returnType.builtIns, Annotations.EMPTY, receiver, parameters, null, returnType, isSuspend) // todo support annotations

    override val inputType: Collection<UnwrappedType> get() = receiver?.let { parameters + it } ?: parameters
    override val outputType: UnwrappedType get() = returnType

    lateinit var resultArguments: List<KotlinCallArgument>
}

class ResolvedCallableReferenceArgument(
        override val outerCall: KotlinCall,
        override val argument: CallableReferenceKotlinCallArgument,
        override val myTypeVariables: List<NewTypeVariable>,
        val callableResolutionCandidate: CallableReferenceCandidate
) : ArgumentWithPostponeResolution() {
    override val inputType: Collection<UnwrappedType> get() = emptyList()
    override val outputType: UnwrappedType? = null
}