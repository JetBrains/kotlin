/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.model.UnionTypeConstructorMarker

class UnionTypeConstructor(typesToUnion: Collection<KotlinType>) : TypeConstructor, UnionTypeConstructorMarker {
    private var alternative: KotlinType? = null

    private constructor(
        typesToUnion: Collection<KotlinType>,
        alternative: KotlinType?,
    ) : this(typesToUnion) {
        this.alternative = alternative
    }

    init {
        assert(!typesToUnion.isEmpty()) { "Attempt to create an empty intersection" }
    }

    private val unionTypes = LinkedHashSet(typesToUnion)
    private val hashCode = unionTypes.hashCode()

    override fun getParameters(): List<TypeParameterDescriptor> = emptyList()

    override fun getSupertypes(): Collection<KotlinType> = TODO()

    // Type should not be rendered in scope's debug name. This may cause performance issues in case of complicated intersection types.
    fun createScopeForKotlinType(): MemberScope = TODO()
//        TypeIntersectionScope.create("member scope for intersection type", unionTypes)

    override fun isFinal(): Boolean = false

    override fun isDenotable(): Boolean = false

    override fun getDeclarationDescriptor(): ClassifierDescriptor? = null

    override fun getBuiltIns(): KotlinBuiltIns =
        unionTypes.iterator().next().constructor.builtIns

    override fun toString(): String = makeDebugNameForIntersectionType()

    fun makeDebugNameForIntersectionType(getProperTypeRelatedToStringify: (KotlinType) -> Any = { it.toString() }): String {
        return unionTypes.sortedBy { getProperTypeRelatedToStringify(it).toString() }
            .joinToString(separator = " & ", prefix = "{", postfix = "}") { getProperTypeRelatedToStringify(it).toString() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnionTypeConstructor) return false

        return unionTypes == other.unionTypes
    }

    @OptIn(TypeRefinement::class)
    fun createType(): SimpleType =
        KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
            TypeAttributes.Empty, this, listOf(), false, this.createScopeForKotlinType()
        ) { kotlinTypeRefiner ->
            TODO()
            // this.refine(kotlinTypeRefiner).createType()
        }

    override fun hashCode(): Int = hashCode

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner) = TODO()
        // transformComponents { it.refine(kotlinTypeRefiner) } ?: this

    /*fun setAlternative(alternative: KotlinType?): IntersectionTypeConstructor {
        return IntersectionTypeConstructor(unionTypes, alternative)
    }*/

    fun getAlternativeType(): KotlinType? = alternative
}
