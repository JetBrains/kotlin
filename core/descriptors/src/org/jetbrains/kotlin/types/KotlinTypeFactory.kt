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
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.scopes.MemberScope

object KotlinTypeFactory {

    @JvmStatic
    fun simpleType(
            annotations: Annotations,
            constructor: TypeConstructor,
            arguments: List<TypeProjection>,
            nullable: Boolean,
            memberScope: MemberScope
    ): SimpleType = KotlinTypeImpl.create(annotations, constructor, nullable, arguments, memberScope)

    @JvmStatic
    fun simpleNotNullType(
            annotations: Annotations,
            descriptor: ClassDescriptor,
            arguments: List<TypeProjection>
    ): SimpleType = KotlinTypeImpl.create(annotations, descriptor.typeConstructor, false, arguments, descriptor.getMemberScope(arguments))

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
    fun flexibleType(lowerBound: SimpleType, upperBound: SimpleType): KotlinType {
        if (lowerBound == upperBound) return lowerBound
        return FlexibleTypeImpl(lowerBound, upperBound)
    }
}