/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.types

import org.jetbrains.kotlin.types.model.RigidTypeMarker
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

internal class FlexibleKType(
    val lowerBound: AbstractKType,
    val upperBound: AbstractKType,
    computeJavaType: (() -> Type)? = null,
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
        get() = false // TODO: investigate the possibility of flexible Nothing

    override val mutableCollectionClass: KClass<*>?
        get() = lowerBound.mutableCollectionClass

    override fun makeNullableAsSpecified(nullable: Boolean): AbstractKType = with(KTypeSystemContext) {
        val lower = (lowerBound as? RigidTypeMarker)?.withNullability(nullable) as? AbstractKType ?: lowerBound
        val upper = (upperBound as? RigidTypeMarker)?.withNullability(nullable) as? AbstractKType ?: upperBound
        if (lower == upper) lower else FlexibleKType(lower, upper)
    }

    override fun makeDefinitelyNotNullAsSpecified(isDefinitelyNotNull: Boolean): AbstractKType =
        // TODO: is everything correct with this implementation?
        if (isDefinitelyNotNull) upperBound else this

    override fun lowerBoundIfFlexible(): AbstractKType? = lowerBound
    override fun upperBoundIfFlexible(): AbstractKType? = upperBound

    override val annotations: List<Annotation>
        get() = lowerBound.annotations
}
