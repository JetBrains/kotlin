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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.TypeIntersectionScope
import java.util.*

class IntersectionTypeConstructor(typesToIntersect: Collection<KotlinType>) : TypeConstructor {
    init {
        assert(!typesToIntersect.isEmpty()) { "Attempt to create an empty intersection" }
    }

    private val intersectedTypes = LinkedHashSet(typesToIntersect)
    private val hashCode = intersectedTypes.hashCode()

    override fun getParameters(): List<TypeParameterDescriptor> = emptyList()

    override fun getSupertypes(): Collection<KotlinType> = intersectedTypes

    fun createScopeForKotlinType(): MemberScope =
        TypeIntersectionScope.create("member scope for intersection type $this", intersectedTypes)

    override fun isFinal(): Boolean = false

    override fun isDenotable(): Boolean = false

    override fun getDeclarationDescriptor(): ClassifierDescriptor? = null

    override fun getBuiltIns(): KotlinBuiltIns =
        intersectedTypes.iterator().next().constructor.builtIns

    override fun toString(): String =
        makeDebugNameForIntersectionType(intersectedTypes)

    private fun makeDebugNameForIntersectionType(resultingTypes: Iterable<KotlinType>): String =
        resultingTypes.sortedBy { it.toString() }.joinToString(separator = " & ", prefix = "{", postfix = "}")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IntersectionTypeConstructor) return false

        return intersectedTypes == other.intersectedTypes
    }

    fun createType() =
        KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
            Annotations.EMPTY, this, listOf(), false, this.createScopeForKotlinType()
        ) { moduleDescriptor: ModuleDescriptor ->
            IntersectionTypeConstructor(intersectedTypes.map {
                it.refine(moduleDescriptor)
            }).createScopeForKotlinType()
        }

    override fun hashCode(): Int = hashCode
}

inline fun IntersectionTypeConstructor.transformComponents(
    predicate: (KotlinType) -> Boolean = { true },
    transform: (KotlinType) -> KotlinType
): IntersectionTypeConstructor? {
    var changed = false
    val newSupertypes = supertypes.map {
        if (predicate(it)) {
            changed = true
            transform(it)
        } else {
            it
        }
    }

    if (!changed) return null

    return IntersectionTypeConstructor(newSupertypes)
}
