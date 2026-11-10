/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.types

import java.lang.reflect.Type
import kotlin.jvm.internal.KTypeBase
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.isNullableType

internal class SimpleKType(
    override val classifier: KClassifier,
    override val arguments: List<KTypeProjection>,
    override val isMarkedNullable: Boolean,
    override val lazyAnnotations: Lazy<List<Annotation>>,
    override val abbreviation: KType?,
    override val isDefinitelyNotNullType: Boolean,
    override val isNothingType: Boolean,
    override val isSuspendFunctionType: Boolean,
    override val mutableCollectionClass: KClass<*>?,
    computeJavaType: Lazy<Type>? = null,
) : AbstractKType(computeJavaType), KTypeBase {
    override fun makeNullableAsSpecified(nullable: Boolean): AbstractKType =
        if (nullable == isMarkedNullable) this
        else SimpleKType(
            classifier.toWrapperClassIfNeeded(nullable), arguments, nullable, lazyAnnotations, abbreviation,
            isDefinitelyNotNullType = isDefinitelyNotNullType && !nullable, isNothingType, isSuspendFunctionType, mutableCollectionClass,
        )

    private fun KClassifier.toWrapperClassIfNeeded(nullable: Boolean): KClassifier {
        if (this !is KClass<*>) return this
        return if (nullable) javaObjectType.kotlin else javaPrimitiveType?.kotlin ?: this
    }

    override fun makeDefinitelyNotNullAsSpecified(isDefinitelyNotNull: Boolean): AbstractKType =
        when {
            isDefinitelyNotNull == isDefinitelyNotNullType -> this
            isDefinitelyNotNull && !makesSenseToBeDefinitelyNotNull() -> makeNullableAsSpecified(false)
            else -> SimpleKType(
                classifier, arguments, isMarkedNullable = isMarkedNullable && !isDefinitelyNotNull, lazyAnnotations, abbreviation,
                isDefinitelyNotNull, isNothingType, isSuspendFunctionType, mutableCollectionClass,
            )
        }

    // See `DefinitelyNotNullType.makesSenseToBeDefinitelyNotNull` in the compiler.
    private fun makesSenseToBeDefinitelyNotNull(): Boolean =
        classifier is KTypeParameter && isNullableType()

    override fun lowerBoundIfFlexible(): AbstractKType? = null
    override fun upperBoundIfFlexible(): AbstractKType? = null

    override val isRawType: Boolean get() = false
}
