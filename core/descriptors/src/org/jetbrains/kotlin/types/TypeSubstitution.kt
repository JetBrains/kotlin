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
import org.jetbrains.kotlin.descriptors.annotations.FilteredAnnotations
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.replaceArguments
import org.jetbrains.kotlin.types.error.ErrorType
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.model.typeConstructor

abstract class TypeSubstitution {
    companion object {
        @JvmField
        val EMPTY: TypeSubstitution = object : TypeSubstitution() {
            override fun get(key: KotlinType): Nothing? = null
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

    fun replaceWithNonApproximating() = object : TypeSubstitution() {
        override fun get(key: KotlinType) = this@TypeSubstitution[key]
        override fun approximateCapturedTypes() = false
        override fun approximateContravariantCapturedTypes() = false
        override fun filterAnnotations(annotations: Annotations) = this@TypeSubstitution.filterAnnotations(annotations)
        override fun prepareTopLevelType(topLevelType: KotlinType, position: Variance) =
            this@TypeSubstitution.prepareTopLevelType(topLevelType, position)

        override fun isEmpty() = this@TypeSubstitution.isEmpty()
    }
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

        @JvmStatic
        fun createByParametersMap(map: Map<TypeParameterDescriptor, TypeProjection>): TypeConstructorSubstitution =
            object : TypeConstructorSubstitution() {
                override fun get(key: TypeConstructor) = map[key.declarationDescriptor]
                override fun isEmpty() = map.isEmpty()
            }

        @JvmStatic
        fun create(kotlinType: KotlinType) = create(kotlinType.constructor, kotlinType.arguments)

        @JvmStatic
        fun create(typeConstructor: TypeConstructor, arguments: List<TypeProjection>): TypeSubstitution {
            val parameters = typeConstructor.parameters

            if (parameters.lastOrNull()?.isCapturedFromOuterDeclaration == true) {
                return createByConstructorsMap(typeConstructor.parameters.map { it.typeConstructor }.zip(arguments).toMap())
            }

            return IndexedParametersSubstitution(parameters, arguments)
        }
    }
}

class SubstitutionWithCapturedTypeApproximation(substitution: TypeSubstitution) : DelegatedTypeSubstitution(substitution) {
    override fun approximateCapturedTypes() = true
}

class SubstitutionWithContravariantCapturedTypeApproximation(substitution: TypeSubstitution) : DelegatedTypeSubstitution(substitution) {
    override fun approximateCapturedTypes() = true
    override fun approximateContravariantCapturedTypes() = true
}

class IndexedParametersSubstitution(
    val parameters: Array<TypeParameterDescriptor>,
    val arguments: Array<TypeProjection>,
    private val approximateContravariantCapturedTypes: Boolean = false
) : TypeSubstitution() {
    init {
        assert(parameters.size <= arguments.size) {
            "Number of arguments should not be less than number of parameters, but: parameters=${parameters.size}, args=${arguments.size}"
        }
    }

    constructor(
        parameters: List<TypeParameterDescriptor>,
        argumentsList: List<TypeProjection>
    ) : this(parameters.toTypedArray(), argumentsList.toTypedArray())

    override fun isEmpty(): Boolean = arguments.isEmpty()

    override fun approximateContravariantCapturedTypes() = approximateContravariantCapturedTypes

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
    newAnnotations: Annotations = annotations,
    newArgumentsForUpperBound: List<TypeProjection> = newArguments
): KotlinType {
    if ((newArguments.isEmpty() || newArguments === arguments) && newAnnotations === annotations) return this

    val newAttributes = attributes.replaceAnnotations(
        // Specially handle FilteredAnnotations here due to FilteredAnnotations.isEmpty()
        if (newAnnotations is FilteredAnnotations && newAnnotations.isEmpty()) Annotations.EMPTY else newAnnotations
    )

    return when (val unwrapped = unwrap()) {
        is FlexibleType -> KotlinTypeFactory.flexibleType(
            unwrapped.lowerBound.replace(newArguments, newAttributes),
            unwrapped.upperBound.replace(newArgumentsForUpperBound, newAttributes)
        )
        is SimpleType -> unwrapped.replace(newArguments, newAttributes)
    }
}

@JvmOverloads
fun SimpleType.replace(
    newArguments: List<TypeProjection> = arguments,
    newAttributes: TypeAttributes = attributes
): SimpleType {
    if (newArguments.isEmpty() && newAttributes === attributes) return this

    if (newArguments.isEmpty()) {
        return replaceAttributes(newAttributes)
    }

    if (this is ErrorType) {
        return replaceArguments(newArguments)
    }

    return KotlinTypeFactory.simpleType(
        newAttributes,
        constructor,
        newArguments,
        isMarkedNullable
    )
}

open class DelegatedTypeSubstitution(val substitution: TypeSubstitution) : TypeSubstitution() {
    override fun get(key: KotlinType) = substitution[key]
    override fun prepareTopLevelType(topLevelType: KotlinType, position: Variance) =
        substitution.prepareTopLevelType(topLevelType, position)

    override fun isEmpty() = substitution.isEmpty()

    override fun approximateCapturedTypes() = substitution.approximateCapturedTypes()
    override fun approximateContravariantCapturedTypes() = substitution.approximateContravariantCapturedTypes()

    override fun filterAnnotations(annotations: Annotations) = substitution.filterAnnotations(annotations)
}

// This method used for transform type to simple type afler substitution
fun KotlinType.asSimpleType(): SimpleType {
    return unwrap() as? SimpleType ?: error("This is should be simple type: $this")
}
