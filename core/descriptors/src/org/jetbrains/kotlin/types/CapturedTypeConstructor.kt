/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.model.CapturedTypeConstructorMarker
import org.jetbrains.kotlin.types.typeUtil.builtIns

class CapturedTypeConstructor(
    val projection: TypeProjection,
    private var supertypesComputation: (() -> List<UnwrappedType>)? = null,
    private val original: CapturedTypeConstructor? = null,
    val typeParameter: TypeParameterDescriptor? = null
) : CapturedTypeConstructorMarker, TypeConstructor {

    constructor(
        projection: TypeProjection,
        supertypes: List<UnwrappedType>,
        original: CapturedTypeConstructor? = null
    ) : this(projection, { supertypes }, original)

    // supertypes from the corresponding type parameter upper bounds
    private val boundSupertypes by lazy(LazyThreadSafetyMode.PUBLICATION) {
        supertypesComputation?.invoke()
    }

    private var projectionSupertype: UnwrappedType? = null

    fun initializeSupertypes(projectionSupertype: UnwrappedType?, boundSupertypes: List<UnwrappedType>) {
        assert(this.supertypesComputation == null) {
            "Already initialized! oldValue = ${this.supertypesComputation}, newValue = $boundSupertypes"
        }
        this.projectionSupertype = projectionSupertype
        this.supertypesComputation = { boundSupertypes }
    }

    override fun getSupertypes(): List<UnwrappedType> = buildList {
        projectionSupertype?.let { add(it) }
        boundSupertypes?.let { addAll(it) }
    }

    fun transformSupertypes(transformation: (UnwrappedType) -> UnwrappedType): Pair<UnwrappedType?, List<UnwrappedType>> {
        val projectionSupertypeTransformed = projectionSupertype?.let(transformation)
        val boundSupertypesTransformed = boundSupertypes?.map(transformation) ?: emptyList()
        return projectionSupertypeTransformed to boundSupertypesTransformed
    }

    override fun getParameters(): List<TypeParameterDescriptor> = emptyList()

    override fun isFinal() = false
    override fun isDenotable() = false
    override fun getDeclarationDescriptor(): ClassifierDescriptor? = null
    override fun getBuiltIns(): KotlinBuiltIns = projection.type.builtIns

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner) =
        CapturedTypeConstructor(
            projection.refine(kotlinTypeRefiner),
            supertypesComputation?.let {
                {
                    supertypes.map { it.refine(kotlinTypeRefiner) }
                }
            },
            original ?: this,
            typeParameter = typeParameter
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CapturedTypeConstructor

        return (original ?: this) === (other.original ?: other)
    }

    override fun hashCode(): Int = original?.hashCode() ?: super.hashCode()
    override fun toString() = "CapturedType($projection)"
}
