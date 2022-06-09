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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.components.ClassicTypeSystemContextForCS
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.model.FixVariableConstraintPositionImpl
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableFromCallableDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.substitute
import org.jetbrains.kotlin.resolve.calls.results.SimpleConstraintSystem
import org.jetbrains.kotlin.types.TypeConstructorSubstitution
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

class SimpleConstraintSystemImpl(
    constraintInjector: ConstraintInjector,
    builtIns: KotlinBuiltIns,
    kotlinTypeRefiner: KotlinTypeRefiner,
    languageVersionSettings: LanguageVersionSettings
) : SimpleConstraintSystem {
    val system = ConstraintSystemImpl(
        constraintInjector, ClassicTypeSystemContextForCS(builtIns, kotlinTypeRefiner), languageVersionSettings
    )
    val csBuilder: ConstraintSystemBuilder =
        system.getBuilder()

    private val resultTypeResolver = ResultTypeResolver(
        constraintInjector.typeApproximator,
        constraintInjector.constraintIncorporator.trivialConstraintTypeInferenceOracle,
        languageVersionSettings
    )

    override fun registerTypeVariables(typeParameters: Collection<TypeParameterMarker>): TypeSubstitutorMarker {

        val substitutionMap = typeParameters.associate {
            requireOrDescribe(it is TypeParameterDescriptor, it)
            val variable = TypeVariableFromCallableDescriptor(it)
            csBuilder.registerVariable(variable)

            it.defaultType.constructor to variable.defaultType.asTypeProjection()
        }
        val substitutor = TypeConstructorSubstitution.createByConstructorsMap(substitutionMap).buildSubstitutor()
        for (typeParameter in typeParameters) {
            requireOrDescribe(typeParameter is TypeParameterDescriptor, typeParameter)
            for (upperBound in typeParameter.upperBounds) {
                addSubtypeConstraint(substitutor.substitute(typeParameter.defaultType), substitutor.substitute(upperBound.unwrap()))
            }
        }
        return substitutor
    }

    fun fixAllTypeVariables() {
        fun pickNextVariable() = system.notFixedTypeVariables.values.first()
        fun hasNotFixedVariable() = system.notFixedTypeVariables.isNotEmpty()

        while (hasNotFixedVariable()) {
            val variableWithConstraints = pickNextVariable()
            val resultType = resultTypeResolver.findResultType(
                system, variableWithConstraints, TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN
            )
            val typeVariable = variableWithConstraints.typeVariable
            system.fixVariable(typeVariable, resultType, FixVariableConstraintPositionImpl(typeVariable, null))
        }
    }

    override fun addSubtypeConstraint(subType: KotlinTypeMarker, superType: KotlinTypeMarker) {
        csBuilder.addSubtypeConstraint(
            subType,
            superType,
            SimpleConstraintSystemConstraintPosition
        )
    }

    override fun hasContradiction() = csBuilder.hasContradiction

    override val context: TypeSystemInferenceExtensionContext
        get() = system
}
