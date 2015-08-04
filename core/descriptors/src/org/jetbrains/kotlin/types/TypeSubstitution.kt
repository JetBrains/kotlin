/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import kotlin.platform.platformStatic

public abstract class TypeSubstitution {
    companion object {
        platformStatic public val EMPTY: TypeSubstitution = object : TypeSubstitution() {
            override fun get(key: JetType) = null
            override fun isEmpty() = true
            override fun toString() = "Empty TypeSubstitution"
        }
    }

    public abstract fun get(key: JetType): TypeProjection?

    public open fun isEmpty(): Boolean = false

    public open fun approximateCapturedTypes(): Boolean = false

    public fun buildSubstitutor(): TypeSubstitutor = TypeSubstitutor.create(this)
}

public abstract class TypeConstructorSubstitution : TypeSubstitution() {
    override fun get(key: JetType) = get(key.constructor)

    public abstract fun get(key: TypeConstructor): TypeProjection?

    companion object {
        platformStatic
        public fun createByConstructorsMap(map: Map<TypeConstructor, TypeProjection>): TypeConstructorSubstitution =
            object : TypeConstructorSubstitution() {
                override fun get(key: TypeConstructor) = map[key]
                override fun isEmpty() = map.isEmpty()
            }

        platformStatic
        public fun createByParametersMap(map: Map<TypeParameterDescriptor, TypeProjection>): TypeConstructorSubstitution =
            object : TypeConstructorSubstitution() {
                override fun get(key: TypeConstructor) = map[key.declarationDescriptor]
                override fun isEmpty() = map.isEmpty()
            }
    }
}

public class IndexedParametersSubstitution(
    private val parameters: Array<TypeParameterDescriptor>,
    private val arguments: Array<TypeProjection>
) : TypeSubstitution() {
    init {
        assert(parameters.size() <= arguments.size()) {
            "Number of arguments should not be less then number of parameters, but: parameters=${parameters.size()}, args=${arguments.size()}"
        }
    }

    constructor(
            typeConstructor: TypeConstructor, argumentsList: List<TypeProjection>
    ) : this(typeConstructor.parameters, argumentsList)

    constructor(
            parameters: List<TypeParameterDescriptor>, argumentsList: List<TypeProjection>
    ) : this(parameters.toTypedArray(), argumentsList.toTypedArray())

    override fun isEmpty(): Boolean = arguments.isEmpty()

    override fun get(key: JetType): TypeProjection? {
        val parameter = key.constructor.declarationDescriptor as? TypeParameterDescriptor ?: return null
        val index = parameter.index

        if (index < parameters.size() && parameters[index].typeConstructor == parameter.typeConstructor) {
            return arguments[index]
        }

        return null
    }
}

public fun JetType.computeNewSubstitution(
    newParameters: List<TypeParameterDescriptor>,
    newArguments: List<TypeProjection>
): TypeSubstitution {
    val previousSubstitution = getSubstitution()
    if (newArguments.isEmpty()) return previousSubstitution

    val newIndexedSubstitution = IndexedParametersSubstitution(newParameters, newArguments)

    // If previous substitution was trivial just replace it with indexed one
    if (previousSubstitution is IndexedParametersSubstitution || previousSubstitution.isEmpty()) {
        return newIndexedSubstitution
    }

    val composedSubstitution = CompositeTypeSubstitution(newIndexedSubstitution, previousSubstitution)

    return composedSubstitution
}

private class CompositeTypeSubstitution(
    private val first: TypeSubstitution,
    private val second: TypeSubstitution
) : TypeSubstitution() {

    override fun get(key: JetType): TypeProjection? {
        val firstResult = first[key] ?: return second[key]
        return second.buildSubstitutor().substitute(firstResult)
    }

    override fun isEmpty() = first.isEmpty() && second.isEmpty()
    //
    override fun approximateCapturedTypes() = first.approximateCapturedTypes() || second.approximateCapturedTypes()
}
