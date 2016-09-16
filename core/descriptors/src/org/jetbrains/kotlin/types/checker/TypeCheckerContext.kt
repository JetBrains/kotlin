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

package org.jetbrains.kotlin.types.checker

import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.SmartSet
import java.util.*

open class TypeCheckerContext(val errorTypeEqualsToAnything: Boolean, val allowedTypeVariable: Boolean = true) {
    protected var argumentsDepth = 0

    private var supertypesLocked = false
    private var supertypesDeque: ArrayDeque<SimpleType>? = null
    private var supertypesSet: MutableSet<SimpleType>? = null

    open fun addSubtypeConstraint(subType: UnwrappedType, superType: UnwrappedType): Boolean? = null

    open fun areEqualTypeConstructors(a: TypeConstructor, b: TypeConstructor): Boolean {
        return a == b
    }

    internal inline fun <T> runWithArgumentsSettings(subArgument: UnwrappedType, f: TypeCheckerContext.() -> T): T {
        if (argumentsDepth > 100) {
            error("Arguments depth is too high. Some related argument: $subArgument")
        }

        argumentsDepth++
        val result = f()
        argumentsDepth--
        return result
    }

    private fun initialize() {
        assert(!supertypesLocked)
        supertypesLocked = true

        if (supertypesDeque == null) {
            supertypesDeque = ArrayDeque()
        }
        if (supertypesSet == null) {
            supertypesSet = SmartSet.create()
        }
    }

    private fun clear() {
        supertypesDeque!!.clear()
        supertypesSet!!.clear()
        supertypesLocked = false
    }

    internal fun anySupertype(
            start: SimpleType,
            predicate: (SimpleType) -> Boolean,
            supertypesPolicy: (SimpleType) -> SupertypesPolicy
    ): Boolean {
        initialize()

        val deque = supertypesDeque!!
        val visitedSupertypes = supertypesSet!!

        deque.push(start)
        while (deque.isNotEmpty()) {
            if (visitedSupertypes.size > 1000) {
                error("Too many supertypes for type: $start. Supertypes = ${visitedSupertypes.joinToString()}")
            }
            val current = deque.pop()

            if (!visitedSupertypes.add(current)) {
                continue
            }

            if (predicate(current)) {
                clear()
                return true
            }

            val policy = supertypesPolicy(current).takeIf { it != SupertypesPolicy.None } ?: continue
            for (supertype in current.constructor.supertypes) deque.add(policy.transformType(supertype))
        }

        clear()
        return false
    }

    internal sealed class SupertypesPolicy {
        abstract fun transformType(type: KotlinType): SimpleType

        object None : SupertypesPolicy() {
            override fun transformType(type: KotlinType) = throw UnsupportedOperationException("Should not be called")
        }

        object UpperIfFlexible : SupertypesPolicy() {
            override fun transformType(type: KotlinType) = type.upperIfFlexible()
        }

        object LowerIfFlexible : SupertypesPolicy() {
            override fun transformType(type: KotlinType) = type.lowerIfFlexible()
        }

        class LowerIfFlexibleWithCustomSubstitutor(val substitutor: TypeSubstitutor): SupertypesPolicy() {
            override fun transformType(type: KotlinType) =
                    substitutor.safeSubstitute(type.lowerIfFlexible(), Variance.INVARIANT).asSimpleType()
        }
    }

    val UnwrappedType.isAllowedTypeVariable: Boolean get() = allowedTypeVariable && constructor is NewTypeVariableConstructor
}