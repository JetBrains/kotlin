/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.types

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktNullability
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KaFe10Type
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.renderForDebugging
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.typeUtil.makeNullable

internal class KaFe10IntersectionType(
    override val fe10Type: SimpleType,
    private val supertypes: Collection<KotlinType>,
    override val analysisContext: Fe10AnalysisContext
) : KaIntersectionType(), KaFe10Type {
    override val conjuncts: List<KaType> by cached {
        val result = ArrayList<KaType>(supertypes.size)
        val isNullable = fe10Type.isMarkedNullable
        for (supertype in supertypes) {
            val mappedSupertype = if (isNullable) supertype.makeNullable() else supertype
            result += mappedSupertype.toKtType(analysisContext)
        }
        return@cached result
    }

    @Deprecated(
        "Use `isMarkedNullable`, `isNullable` or `hasFlexibleNullability` instead. See KDocs for the migration guide",
        replaceWith = ReplaceWith("this.isMarkedNullable")
    )
    @Suppress("Deprecation")
    override val nullability: KaTypeNullability
        get() = withValidityAssertion { fe10Type.ktNullability }

    override val abbreviation: KaUsualClassType?
        get() = withValidityAssertion { null }

    override fun toString(): String {
        return fe10Type.renderForDebugging(analysisContext)
    }

    @KaExperimentalApi
    override fun createPointer(): KaTypePointer<KaIntersectionType> = withValidityAssertion {
        throw NotImplementedError("Type pointers are not implemented for FE 1.0")
    }
}
