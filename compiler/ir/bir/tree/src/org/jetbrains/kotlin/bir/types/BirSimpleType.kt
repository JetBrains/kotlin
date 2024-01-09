/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.types

import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.symbols.BirClassifierSymbol
import org.jetbrains.kotlin.bir.symbols.BirTypeParameterSymbol
import org.jetbrains.kotlin.bir.types.impl.BirTypeBase
import org.jetbrains.kotlin.bir.util.FqNameEqualityChecker
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.model.TypeArgumentListMarker

abstract class BirSimpleType(kotlinType: KotlinType?) : BirTypeBase(kotlinType), SimpleTypeMarker, TypeArgumentListMarker {
    abstract val classifier: BirClassifierSymbol

    /**
     * If type is explicitly marked as nullable, [nullability] is [SimpleTypeNullability.MARKED_NULLABLE]
     *
     * If classifier is type parameter, not marked as nullable, but can store null values,
     * if corresponding argument would be nullable, [nullability] is [SimpleTypeNullability.NOT_SPECIFIED]
     *
     * If type can't store null values, [nullability] is [SimpleTypeNullability.DEFINITELY_NOT_NULL]
     *
     * Direct usages of this property should be avoided in most cases. Use relevant util functions instead.
     *
     * In most cases one of following is needed:
     *
     * Use [BirType.isNullable] to check if null value is possible for this type
     *
     * Use [BirType.isMarkedNullable] to check if type is marked with question mark in code
     *
     * Use [BirType.mergeNullability] to apply nullability of type parameter to actual type argument in type substitutions
     *
     * Use [BirType.makeNotNull] or [BirType.makeNullable] to transfer nullability from one type to another
     */
    abstract val nullability: SimpleTypeNullability
    abstract val arguments: List<BirTypeArgument>
    abstract val abbreviation: BirTypeAbbreviation?

    override val variance: Variance
        get() = Variance.INVARIANT
}

abstract class BirAbstractSimpleType(kotlinType: KotlinType?) : BirSimpleType(kotlinType) {
    abstract override val classifier: BirClassifierSymbol
    abstract override val nullability: SimpleTypeNullability
    abstract override val arguments: List<BirTypeArgument>
    abstract override val annotations: List<BirConstructorCall>
    abstract override val abbreviation: BirTypeAbbreviation?

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        return other is BirAbstractSimpleType &&
                FqNameEqualityChecker.areEqual(classifier, other.classifier) &&
                nullability == other.nullability &&
                arguments == other.arguments
    }

    override fun hashCode(): Int =
        (FqNameEqualityChecker.getHashCode(classifier) * 31 +
                nullability.hashCode()) * 31 +
                arguments.hashCode()
}

class BirSimpleTypeImpl(
    kotlinType: KotlinType?,
    override val classifier: BirClassifierSymbol,
    nullability: SimpleTypeNullability,
    override val arguments: List<BirTypeArgument>,
    override val annotations: List<BirConstructorCall>,
    override val abbreviation: BirTypeAbbreviation? = null
) : BirAbstractSimpleType(kotlinType) {
    constructor(
        classifier: BirClassifierSymbol,
        nullability: SimpleTypeNullability,
        arguments: List<BirTypeArgument>,
        annotations: List<BirConstructorCall>,
        abbreviation: BirTypeAbbreviation? = null
    ) : this(null, classifier, nullability, arguments, annotations, abbreviation)

    constructor(
        classifier: BirClassifierSymbol,
        hasQuestionMark: Boolean,
        arguments: List<BirTypeArgument>,
        annotations: List<BirConstructorCall>,
        abbreviation: BirTypeAbbreviation? = null
    ) : this(null, classifier, SimpleTypeNullability.fromHasQuestionMark(hasQuestionMark), arguments, annotations, abbreviation)

    override val nullability =
        if (classifier !is BirTypeParameterSymbol && nullability == SimpleTypeNullability.NOT_SPECIFIED)
            SimpleTypeNullability.DEFINITELY_NOT_NULL
        else
            nullability
}
