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

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import java.lang.IllegalStateException

object KotlinTypeFactory {
    private fun computeMemberScope(constructor: TypeConstructor, arguments: List<TypeProjection>): MemberScope {
        val descriptor = constructor.declarationDescriptor
        return when (descriptor) {
            is TypeParameterDescriptor -> descriptor.getDefaultType().memberScope
            is ClassDescriptor ->  descriptor.getMemberScope(TypeConstructorSubstitution.create(constructor, arguments))
            is TypeAliasDescriptor -> ErrorUtils.createErrorScope("Scope for abbreviation: ${descriptor.name}", true)
            else -> throw IllegalStateException("Unsupported classifier: $descriptor for constructor: $constructor")
        }
    }

    @JvmStatic
    fun simpleType(
            annotations: Annotations,
            constructor: TypeConstructor,
            arguments: List<TypeProjection>,
            nullable: Boolean,
            memberScope: MemberScope = computeMemberScope(constructor, arguments)
    ): SimpleType = SimpleTypeImpl(annotations, constructor, arguments, nullable, memberScope)

    @JvmStatic
    fun simpleNotNullType(
            annotations: Annotations,
            descriptor: ClassDescriptor,
            arguments: List<TypeProjection>
    ): SimpleType = SimpleTypeImpl(annotations, descriptor.typeConstructor, arguments, false, descriptor.getMemberScope(arguments))

    @JvmStatic
    fun functionType(
            annotations: Annotations,
            descriptor: ClassDescriptor,
            arguments: List<TypeProjection>,
            parameterNames: List<Name>?
    ): SimpleType {
        val simpleType = simpleNotNullType(annotations, descriptor, arguments)
        if (parameterNames == null || parameterNames.all { it.isSpecial }) {
            return simpleType
        }
        else {
            return FunctionType(simpleType, parameterNames)
        }
    }

    @JvmStatic
    fun simpleType(
            baseType: SimpleType,
            annotations: Annotations = baseType.annotations,
            constructor: TypeConstructor = baseType.constructor,
            arguments: List<TypeProjection> = baseType.arguments,
            nullable: Boolean = baseType.isMarkedNullable,
            memberScope: MemberScope = baseType.memberScope
    ): SimpleType = simpleType(annotations, constructor, arguments, nullable, memberScope)

    @JvmStatic
    fun flexibleType(lowerBound: SimpleType, upperBound: SimpleType): UnwrappedType {
        if (lowerBound == upperBound) return lowerBound
        return FlexibleTypeImpl(lowerBound, upperBound)
    }
}

private class SimpleTypeImpl(
        override val annotations: Annotations,
        override val constructor: TypeConstructor,
        override val arguments: List<TypeProjection>,
        override val isMarkedNullable: Boolean,
        override val memberScope: MemberScope
) : SimpleType() {
    override fun replaceAnnotations(newAnnotations: Annotations) = SimpleTypeImpl(newAnnotations, constructor, arguments, isMarkedNullable, memberScope)
    override fun makeNullableAsSpecified(newNullability: Boolean) = SimpleTypeImpl(annotations, constructor, arguments, newNullability, memberScope)

    override val isError: Boolean
        get() = false

    init {
        if (memberScope is ErrorUtils.ErrorScope) {
            throw IllegalStateException("SimpleTypeImpl should not be created for error type: $memberScope\n$constructor")
        }
    }
}