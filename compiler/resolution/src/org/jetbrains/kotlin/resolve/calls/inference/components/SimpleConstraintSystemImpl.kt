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

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallKind
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableFromCallableDescriptor
import org.jetbrains.kotlin.resolve.calls.model.KotlinCall
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.model.ReceiverKotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.model.TypeArgument
import org.jetbrains.kotlin.resolve.calls.results.SimpleConstraintSystem
import org.jetbrains.kotlin.types.TypeConstructorSubstitution
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import java.lang.UnsupportedOperationException


class SimpleConstraintSystemImpl(constraintInjector: ConstraintInjector, resultTypeResolver: ResultTypeResolver) : SimpleConstraintSystem {
    val csBuilder: ConstraintSystemBuilder = NewConstraintSystemImpl(constraintInjector, resultTypeResolver).getBuilder()

    override fun registerTypeVariables(typeParameters: Collection<TypeParameterDescriptor>): TypeSubstitutor {
        val substitutionMap = typeParameters.associate {
            val variable = TypeVariableFromCallableDescriptor(ThrowableKotlinCall, it)
            csBuilder.registerVariable(variable)

            it.defaultType.constructor to variable.defaultType.asTypeProjection()
        }
        return TypeConstructorSubstitution.createByConstructorsMap(substitutionMap).buildSubstitutor()
    }

    override fun addSubtypeConstraint(subType: UnwrappedType, superType: UnwrappedType) {
        csBuilder.addSubtypeConstraint(subType, superType, SimpleConstraintSystemConstraintPosition)
    }

    override fun hasContradiction() = csBuilder.hasContradiction

    private object ThrowableKotlinCall : KotlinCall {
        override val callKind: KotlinCallKind get() = throw UnsupportedOperationException()
        override val explicitReceiver: ReceiverKotlinCallArgument? get() = throw UnsupportedOperationException()
        override val name: Name get() = throw UnsupportedOperationException()
        override val typeArguments: List<TypeArgument> get() = throw UnsupportedOperationException()
        override val argumentsInParenthesis: List<KotlinCallArgument> get() = throw UnsupportedOperationException()
        override val externalArgument: KotlinCallArgument? get() = throw UnsupportedOperationException()
        override val isInfixCall: Boolean get() = throw UnsupportedOperationException()
        override val isOperatorCall: Boolean get() = throw UnsupportedOperationException()
        override val isSupertypeConstructorCall: Boolean get() = throw UnsupportedOperationException()
    }
}