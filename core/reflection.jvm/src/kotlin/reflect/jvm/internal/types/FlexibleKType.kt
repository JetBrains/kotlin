/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.types

import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

internal class FlexibleKType private constructor(
    val lowerBound: AbstractKType,
    val upperBound: AbstractKType,
    override val isRawType: Boolean,
    computeJavaType: (() -> Type)?,
) : AbstractKType(computeJavaType) {
    override val classifier: KClassifier?
        get() = lowerBound.classifier

    override val arguments: List<KTypeProjection>
        get() = lowerBound.arguments

    override val isMarkedNullable: Boolean
        get() = lowerBound.isMarkedNullable

    override val abbreviation: KType?
        get() = null

    override val isDefinitelyNotNullType: Boolean
        get() = false

    override val isNothingType: Boolean
        get() = false

    override val isSuspendFunctionType: Boolean
        get() = false

    override val mutableCollectionClass: KClass<*>?
        get() = lowerBound.mutableCollectionClass

    override fun makeNullableAsSpecified(nullable: Boolean): AbstractKType =
        create(lowerBound.makeNullableAsSpecified(nullable), upperBound.makeNullableAsSpecified(nullable), isRawType)

    override fun makeDefinitelyNotNullAsSpecified(isDefinitelyNotNull: Boolean): AbstractKType =
        create(
            lowerBound.makeDefinitelyNotNullAsSpecified(isDefinitelyNotNull),
            upperBound.makeDefinitelyNotNullAsSpecified(isDefinitelyNotNull),
            isRawType,
        )

    override fun lowerBoundIfFlexible(): AbstractKType? = lowerBound
    override fun upperBoundIfFlexible(): AbstractKType? = upperBound

    override val annotations: List<Annotation>
        get() = lowerBound.annotations

    companion object {
        fun create(
            lowerBound: AbstractKType,
            upperBound: AbstractKType,
            isRawType: Boolean,
            computeJavaType: (() -> Type)? = null,
        ): AbstractKType =
            if (lowerBound == upperBound) lowerBound else FlexibleKType(lowerBound, upperBound, isRawType, computeJavaType)
    }
}
