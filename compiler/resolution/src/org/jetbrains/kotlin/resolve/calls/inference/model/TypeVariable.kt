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
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.model.PostponableKotlinCallArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.hasOnlyInputTypesAnnotation
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.TypeVariableMarker

sealed class TypeVariable(
    builtIns: KotlinBuiltIns,
    name: String,
    originalTypeParameter: TypeParameterDescriptor? = null
) : TypeVariableMarker {
    val freshTypeConstructor = TypeVariableTypeConstructor(builtIns, name, originalTypeParameter)

    // member scope is used if we have receiver with type TypeVariable(T)
    // todo add to member scope methods from supertypes for type variable
    val defaultType: SimpleType = freshTypeConstructor.typeForTypeVariable()
    abstract fun hasOnlyInputTypesAnnotation(): Boolean

    override fun toString() = freshTypeConstructor.toString()
}

class TypeVariableFromCallableDescriptor(
    val originalTypeParameter: TypeParameterDescriptor
) : TypeVariable(originalTypeParameter.builtIns, originalTypeParameter.name.identifier, originalTypeParameter) {
    override fun hasOnlyInputTypesAnnotation(): Boolean = originalTypeParameter.hasOnlyInputTypesAnnotation()
}

class TypeVariableForLambdaReturnType(
    builtIns: KotlinBuiltIns,
    name: String
) : TypeVariable(builtIns, name) {
    override fun hasOnlyInputTypesAnnotation(): Boolean = false
}

class TypeVariableForLambdaParameterType(
    val atom: PostponableKotlinCallArgument,
    val index: Int,
    builtIns: KotlinBuiltIns,
    name: String
) : TypeVariable(builtIns, name) {
    override fun hasOnlyInputTypesAnnotation(): Boolean = false
}

class TypeVariableForCallableReferenceReturnType(
    builtIns: KotlinBuiltIns,
    name: String
) : TypeVariable(builtIns, name) {
    override fun hasOnlyInputTypesAnnotation(): Boolean = false
}

class TypeVariableForCallableReferenceParameterType(
    builtIns: KotlinBuiltIns,
    name: String
) : TypeVariable(builtIns, name) {
    override fun hasOnlyInputTypesAnnotation(): Boolean = false
}

fun TypeConstructor.typeForTypeVariable(): SimpleType {
    require(this is TypeVariableTypeConstructor)
    return KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
        TypeAttributes.Empty, this, arguments = emptyList(),
        nullable = false, memberScope = builtIns.any.unsubstitutedMemberScope
    )
}
