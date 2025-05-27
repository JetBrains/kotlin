/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.types

import java.lang.reflect.Type
import kotlin.jvm.internal.KTypeBase
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

internal class SimpleKType(
    override val classifier: KClassifier,
    override val arguments: List<KTypeProjection>,
    override val isMarkedNullable: Boolean,
    override val annotations: List<Annotation>,
    override val abbreviation: KType?,
    override val isDefinitelyNotNullType: Boolean,
    override val isNothingType: Boolean,
    override val isSuspendFunctionType: Boolean,
    override val mutableCollectionClass: KClass<*>?,
    computeJavaType: (() -> Type)? = null,
) : AbstractKType(computeJavaType), KTypeBase {
    override fun makeNullableAsSpecified(nullable: Boolean): AbstractKType = SimpleKType(
        classifier, arguments, nullable, annotations, abbreviation, isDefinitelyNotNullType = false, isNothingType, isSuspendFunctionType,
        mutableCollectionClass,
    )

    override fun makeDefinitelyNotNullAsSpecified(isDefinitelyNotNull: Boolean): AbstractKType = SimpleKType(
        classifier, arguments, isMarkedNullable = isMarkedNullable && !isDefinitelyNotNull, annotations, abbreviation, isDefinitelyNotNull,
        isNothingType, isSuspendFunctionType, mutableCollectionClass,
    )

    override fun lowerBoundIfFlexible(): AbstractKType? = null
    override fun upperBoundIfFlexible(): AbstractKType? = null

    override val isRawType: Boolean get() = false
}
