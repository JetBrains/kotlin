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

public interface TypeCapability

public interface TypeCapabilities {
    object NONE : TypeCapabilities {
        override fun <T : TypeCapability> getCapability(capabilityClass: Class<T>): T? = null
    }

    fun <T : TypeCapability> getCapability(capabilityClass: Class<T>): T?
}

public inline fun <reified T : TypeCapability> KtType.getCapability(): T? = getCapability(javaClass<T>())

public interface Specificity : TypeCapability {

    public enum class Relation {
        LESS_SPECIFIC,
        MORE_SPECIFIC,
        DONT_KNOW
    }

    public fun getSpecificityRelationTo(otherType: KtType): Relation
}

fun KtType.getSpecificityRelationTo(otherType: KtType) =
        this.getCapability(javaClass<Specificity>())?.getSpecificityRelationTo(otherType) ?: Specificity.Relation.DONT_KNOW

fun oneMoreSpecificThanAnother(a: KtType, b: KtType) =
        a.getSpecificityRelationTo(b) != Specificity.Relation.DONT_KNOW || b.getSpecificityRelationTo(a) != Specificity.Relation.DONT_KNOW

// To facilitate laziness, any KtType implementation may inherit from this trait,
// even if it turns out that the type an instance represents is not actually a type variable
// (i.e. it is not derived from a type parameter), see isTypeVariable
public interface CustomTypeVariable : TypeCapability {
    public val isTypeVariable: Boolean

    // If typeParameterDescriptor != null <=> isTypeVariable == true, this is not a type variable
    public val typeParameterDescriptor: TypeParameterDescriptor?

    // Throws an exception when isTypeVariable == false
    public fun substitutionResult(replacement: KtType): KtType
}

public fun KtType.isCustomTypeVariable(): Boolean = this.getCapability(javaClass<CustomTypeVariable>())?.isTypeVariable ?: false
public fun KtType.getCustomTypeVariable(): CustomTypeVariable? =
        this.getCapability(javaClass<CustomTypeVariable>())?.let {
            if (it.isTypeVariable) it else null
        }

public interface SubtypingRepresentatives : TypeCapability {
    public val subTypeRepresentative: KtType
    public val superTypeRepresentative: KtType

    public fun sameTypeConstructor(type: KtType): Boolean
}

public fun KtType.getSubtypeRepresentative(): KtType =
        this.getCapability(javaClass<SubtypingRepresentatives>())?.subTypeRepresentative ?: this

public fun KtType.getSupertypeRepresentative(): KtType =
        this.getCapability(javaClass<SubtypingRepresentatives>())?.superTypeRepresentative ?: this

public fun sameTypeConstructors(first: KtType, second: KtType): Boolean {
    val typeRangeCapability = javaClass<SubtypingRepresentatives>()
    return first.getCapability(typeRangeCapability)?.sameTypeConstructor(second) ?: false
           || second.getCapability(typeRangeCapability)?.sameTypeConstructor(first) ?: false
}

interface CustomSubstitutionCapability : TypeCapability {
    public val substitution: TypeSubstitution
}
