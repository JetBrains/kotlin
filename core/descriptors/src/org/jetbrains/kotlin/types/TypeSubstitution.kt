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

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations

abstract class TypeSubstitution {
    companion object {
        @JvmField val EMPTY: TypeSubstitution = object : TypeSubstitution() {
            override fun get(key: KotlinType) = null
            override fun isEmpty() = true
            override fun toString() = "Empty TypeSubstitution"
        }
    }

    abstract operator fun get(key: KotlinType): TypeProjection?

    // This can be used to perform preliminary manipulations with top-level types
    open fun prepareTopLevelType(topLevelType: KotlinType, position: Variance): KotlinType = topLevelType

    open fun isEmpty(): Boolean = false

    open fun approximateCapturedTypes(): Boolean = false
    open fun approximateContravariantCapturedTypes(): Boolean = false

    open fun filterAnnotations(annotations: Annotations) = annotations

    fun buildSubstitutor(): TypeSubstitutor = TypeSubstitutor.create(this)
}

abstract class TypeConstructorSubstitution : TypeSubstitution() {
    override fun get(key: KotlinType) = get(key.constructor)

    abstract fun get(key: TypeConstructor): TypeProjection?

    companion object {
        @JvmStatic
        @JvmOverloads
        fun createByConstructorsMap(
                map: Map<TypeConstructor, TypeProjection>,
                approximateCapturedTypes: Boolean = false
        ): TypeConstructorSubstitution =
            object : TypeConstructorSubstitution() {
                override fun get(key: TypeConstructor) = map[key]
                override fun isEmpty() = map.isEmpty()
                override fun approximateCapturedTypes() = approximateCapturedTypes
            }

        @JvmStatic fun createByParametersMap(map: Map<TypeParameterDescriptor, TypeProjection>): TypeConstructorSubstitution =
            object : TypeConstructorSubstitution() {
                override fun get(key: TypeConstructor) = map[key.declarationDescriptor]
                override fun isEmpty() = map.isEmpty()
            }

        @JvmStatic fun create(kotlinType: KotlinType) = create(kotlinType.constructor, kotlinType.arguments)

        @JvmStatic fun create(typeConstructor: TypeConstructor, arguments: List<TypeProjection>): TypeSubstitution {
            val parameters = typeConstructor.parameters

            if (parameters.lastOrNull()?.isCapturedFromOuterDeclaration ?: false) {
                return createByConstructorsMap(typeConstructor.parameters.map { it.typeConstructor }.zip(arguments).toMap())
            }

            return IndexedParametersSubstitution(parameters, arguments)
        }
    }
}

class IndexedParametersSubstitution(
    val parameters: Array<TypeParameterDescriptor>,
    val arguments: Array<TypeProjection>,
    private val approximateCapturedTypes: Boolean = false
) : TypeSubstitution() {
    init {
        assert(parameters.size <= arguments.size) {
            "Number of arguments should not be less then number of parameters, but: parameters=${parameters.size}, args=${arguments.size}"
        }
    }

    constructor(
            parameters: List<TypeParameterDescriptor>, argumentsList: List<TypeProjection>
    ) : this(parameters.toTypedArray(), argumentsList.toTypedArray())

    override fun isEmpty(): Boolean = arguments.isEmpty()

    override fun approximateContravariantCapturedTypes() = approximateCapturedTypes

    override fun get(key: KotlinType): TypeProjection? {
        val parameter = key.constructor.declarationDescriptor as? TypeParameterDescriptor ?: return null
        val index = parameter.index

        if (index < parameters.size && parameters[index].typeConstructor == parameter.typeConstructor) {
            return arguments[index]
        }

        return null
    }
}

@JvmOverloads
fun KotlinType.replace(
        newArguments: List<TypeProjection> = arguments,
        newAnnotations: Annotations = annotations
): KotlinType {
    if ((newArguments.isEmpty() || newArguments === arguments) && newAnnotations === annotations) return this

    val unwrapped = unwrap()
    return when(unwrapped) {
        is FlexibleType -> KotlinTypeFactory.flexibleType(unwrapped.lowerBound.replace(newArguments, newAnnotations),
                                                          unwrapped.upperBound.replace(newArguments, newAnnotations))
        is SimpleType -> unwrapped.replace(newArguments, newAnnotations)
    }
}

@JvmOverloads
fun SimpleType.replace(
        newArguments: List<TypeProjection> = arguments,
        newAnnotations: Annotations = annotations
): SimpleType {
    if (newArguments.isEmpty() && newAnnotations === annotations) return this

    if (newArguments.isEmpty()) {
        return KotlinTypeFactory.simpleType(
                newAnnotations,
                constructor,
                arguments,
                isMarkedNullable,
                memberScope
        )
    }

    return KotlinTypeFactory.simpleType(
            newAnnotations,
            constructor,
            newArguments,
            isMarkedNullable
    )
}

open class DelegatedTypeSubstitution(val substitution: TypeSubstitution): TypeSubstitution() {
    override fun get(key: KotlinType) = substitution[key]
    override fun prepareTopLevelType(topLevelType: KotlinType, position: Variance) = substitution.prepareTopLevelType(topLevelType, position)

    override fun isEmpty() = substitution.isEmpty()

    override fun approximateCapturedTypes() = substitution.approximateCapturedTypes()
    override fun approximateContravariantCapturedTypes() = substitution.approximateContravariantCapturedTypes()

    override fun filterAnnotations(annotations: Annotations) = substitution.filterAnnotations(annotations)
}

// This method used for transform type to simple type afler substitution
fun KotlinType.asSimpleType(): SimpleType {
    return unwrap() as? SimpleType ?: error("This is should be simple type: $this")
}
