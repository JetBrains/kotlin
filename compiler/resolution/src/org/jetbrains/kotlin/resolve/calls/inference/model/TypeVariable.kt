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

package org.jetbrains.kotlin.resolve.calls.inference.model

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.calls.model.LambdaKotlinCallArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.hasOnlyInputTypesAnnotation
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.checker.NewTypeVariableConstructor
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.types.refinement.TypeRefinement


class TypeVariableTypeConstructor(private val builtIns: KotlinBuiltIns, val debugName: String) : TypeConstructor,
    NewTypeVariableConstructor {
    override fun getParameters(): List<TypeParameterDescriptor> = emptyList()
    override fun getSupertypes(): Collection<KotlinType> = emptyList()
    override fun isFinal(): Boolean = false
    override fun isDenotable(): Boolean = false
    override fun getDeclarationDescriptor(): ClassifierDescriptor? = null

    override fun getBuiltIns() = builtIns

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner): TypeConstructor = this

    override fun toString() = "TypeVariable($debugName)"
}

sealed class NewTypeVariable(builtIns: KotlinBuiltIns, name: String) : TypeVariableMarker {
    val freshTypeConstructor: TypeConstructor = TypeVariableTypeConstructor(builtIns, name)

    // member scope is used if we have receiver with type TypeVariable(T)
    // todo add to member scope methods from supertypes for type variable
    val defaultType: SimpleType = KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
        Annotations.EMPTY, freshTypeConstructor, arguments = emptyList(),
        nullable = false, memberScope = builtIns.any.unsubstitutedMemberScope
    )

    abstract fun hasOnlyInputTypesAnnotation(): Boolean

    override fun toString() = freshTypeConstructor.toString()
}

class TypeVariableFromCallableDescriptor(
    val originalTypeParameter: TypeParameterDescriptor
) : NewTypeVariable(originalTypeParameter.builtIns, originalTypeParameter.name.identifier) {
    override fun hasOnlyInputTypesAnnotation(): Boolean = originalTypeParameter.hasOnlyInputTypesAnnotation()
}

class TypeVariableForLambdaReturnType(
    val lambdaArgument: LambdaKotlinCallArgument,
    builtIns: KotlinBuiltIns,
    name: String
) : NewTypeVariable(builtIns, name) {
    override fun hasOnlyInputTypesAnnotation(): Boolean = false
}
