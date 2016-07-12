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

interface TypeCapability

interface TypeCapabilities {
    object NONE : TypeCapabilities {
        override fun <T : TypeCapability> getCapability(capabilityClass: Class<T>): T? = null
    }

    fun <T : TypeCapability> getCapability(capabilityClass: Class<T>): T?
}

class CompositeTypeCapabilities(private val first: TypeCapabilities, private val second: TypeCapabilities) : TypeCapabilities {
    override fun <T : TypeCapability> getCapability(capabilityClass: Class<T>): T? =
            first.getCapability(capabilityClass) ?: second.getCapability(capabilityClass)
}

class SingletonTypeCapabilities(private val clazz: Class<*>, private val typeCapability: TypeCapability) : TypeCapabilities {
    override fun <T : TypeCapability> getCapability(capabilityClass: Class<T>): T? {
        @Suppress("UNCHECKED_CAST")
        if (capabilityClass == clazz) return typeCapability as T
        return null
    }
}

fun <T : TypeCapability> TypeCapabilities.addCapability(clazz: Class<T>, typeCapability: T): TypeCapabilities {
    if (getCapability(clazz) === typeCapability) return this
    val newCapabilities = SingletonTypeCapabilities(clazz, typeCapability)
    if (this === TypeCapabilities.NONE) return newCapabilities

    return CompositeTypeCapabilities(this, newCapabilities)
}

inline fun <reified T : TypeCapability> KotlinType.getCapability(): T? = getCapability(T::class.java)


// To facilitate laziness, any KotlinType implementation may inherit from this trait,
// even if it turns out that the type an instance represents is not actually a type variable
// (i.e. it is not derived from a type parameter), see isTypeVariable
interface CustomTypeVariable : TypeCapability {
    val isTypeVariable: Boolean

    // Throws an exception when isTypeVariable == false
    fun substitutionResult(replacement: KotlinType): KotlinType
}

fun KotlinType.isCustomTypeVariable(): Boolean = this.getCapability(CustomTypeVariable::class.java)?.isTypeVariable ?: false
fun KotlinType.getCustomTypeVariable(): CustomTypeVariable? =
        this.getCapability(CustomTypeVariable::class.java)?.let {
            if (it.isTypeVariable) it else null
        }

interface SubtypingRepresentatives : TypeCapability {
    val subTypeRepresentative: KotlinType
    val superTypeRepresentative: KotlinType

    fun sameTypeConstructor(type: KotlinType): Boolean
}

fun KotlinType.getSubtypeRepresentative(): KotlinType =
        this.getCapability(SubtypingRepresentatives::class.java)?.subTypeRepresentative ?: this

fun KotlinType.getSupertypeRepresentative(): KotlinType =
        this.getCapability(SubtypingRepresentatives::class.java)?.superTypeRepresentative ?: this

fun sameTypeConstructors(first: KotlinType, second: KotlinType): Boolean {
    val typeRangeCapability = SubtypingRepresentatives::class.java
    return first.getCapability(typeRangeCapability)?.sameTypeConstructor(second) ?: false
           || second.getCapability(typeRangeCapability)?.sameTypeConstructor(first) ?: false
}

interface AbbreviatedType : TypeCapability {
    val abbreviatedType : KotlinType
}

fun KotlinType.hasAbbreviatedType(): Boolean =
        getCapability(AbbreviatedType::class.java) != null

fun KotlinType.getAbbreviatedType(): KotlinType? =
        getCapability(AbbreviatedType::class.java)?.abbreviatedType
