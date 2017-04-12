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

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.checker.NewCapturedTypeConstructor
import org.jetbrains.kotlin.types.checker.intersectTypes

interface NewTypeSubstitutor {
    fun substituteNotNullTypeWithConstructor(constructor: TypeConstructor): UnwrappedType?
    val isEmpty: Boolean

    fun safeSubstitute(type: UnwrappedType): UnwrappedType = substitute(type) ?: type

    // null means that this type isn't changed
    fun substitute(type: UnwrappedType): UnwrappedType? =
            when (type) {
                is SimpleType -> substitute(type)
                is FlexibleType -> if (type is DynamicType || type is RawType) {
                    null
                }
                else {
                    val lowerBound = substitute(type.lowerBound)
                    val upperBound = substitute(type.upperBound)
                    if (lowerBound == null && upperBound == null) {
                        null
                    }
                    else {
                        // todo discuss lowerIfFlexible and upperIfFlexible
                        FlexibleTypeImpl(lowerBound?.lowerIfFlexible() ?: type.lowerBound, upperBound?.upperIfFlexible() ?: type.upperBound)
                    }
                }
            }

    fun substitute(type: SimpleType): UnwrappedType? {
        if (type.isError) return null

        if (type.arguments.isNotEmpty()) {
            return substituteParametrizedType(type)
        }

        val typeConstructor = type.constructor

        if (typeConstructor is NewCapturedTypeConstructor) {
            assert(type is NewCapturedType) { // KT-16147
                "Type is inconsistent -- somewhere we create type with typeConstructor = $typeConstructor " +
                "and class: ${type::class.java.canonicalName}. type.toString() = $type"
            }
            val lower = (type as NewCapturedType).lowerType?.let { substitute(it) }
            if (lower != null) throw IllegalStateException("Illegal type substitutor: $this, " +
                                                           "because for captured type '$type' lower type approximation should be null, but it is: '$lower'," +
                                                           "original lower type: '${type.lowerType}")

            type.constructor.supertypes.forEach { supertype ->
                substitute(supertype)?.let {
                    throw IllegalStateException("Illegal type substitutor: $this, " +
                                                "because for captured type '$type' supertype approximation should be null, but it is: '$supertype'," +
                                                "original supertype: '$supertype'")
                }
            }

            return null
        }

        if (typeConstructor is IntersectionTypeConstructor) {
            var thereIsChanges = false
            val newTypes = typeConstructor.supertypes.map {
                substitute(it.unwrap())?.apply { thereIsChanges = true } ?: it.unwrap()
            }
            if (!thereIsChanges) return null
            return intersectTypes(newTypes).let { if (type.isMarkedNullable) it.makeNullableAsSpecified(true) else it }
        }

        // simple classifier type
        val replacement = substituteNotNullTypeWithConstructor(typeConstructor) ?: return null

        return if (type.isMarkedNullable) replacement.makeNullableAsSpecified(true) else replacement
    }

    private fun substituteParametrizedType(type: SimpleType): UnwrappedType? {
        val parameters = type.constructor.parameters
        val arguments = type.arguments
        if (parameters.size != arguments.size) {
            // todo error type or exception?
            return ErrorUtils.createErrorType("Inconsistent type: $type (parameters.size = ${parameters.size}, arguments.size = ${arguments.size})")
        }
        val newArguments = arrayOfNulls<TypeProjection?>(arguments.size)

        for (index in arguments.indices) {
            val argument = arguments[index]

            if (argument.isStarProjection) continue
            val substitutedArgumentType = substitute(argument.type.unwrap()) ?: continue

            newArguments[index] = TypeProjectionImpl(argument.projectionKind, substitutedArgumentType)
        }

        if (newArguments.all { it == null }) return null

        val newArgumentsList = arguments.mapIndexed { index, oldArgument -> newArguments[index] ?: oldArgument }
        return type.replace(newArgumentsList)
    }
}

class NewTypeSubstitutorByConstructorMap(val map: Map<TypeConstructor, UnwrappedType>) : NewTypeSubstitutor {
    override val isEmpty get() = map.isEmpty()
    override fun substituteNotNullTypeWithConstructor(constructor: TypeConstructor): UnwrappedType? = map[constructor]
}