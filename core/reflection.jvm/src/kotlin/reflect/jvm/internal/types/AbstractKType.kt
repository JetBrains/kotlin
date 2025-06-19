/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.types

import org.jetbrains.kotlin.types.model.DefinitelyNotNullTypeMarker
import org.jetbrains.kotlin.types.model.FlexibleTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.model.TypeArgumentListMarker
import java.lang.reflect.Type
import kotlin.jvm.internal.KTypeBase
import kotlin.reflect.KType
import kotlin.reflect.jvm.internal.ReflectProperties
import kotlin.reflect.jvm.internal.ReflectionObjectRenderer

internal abstract class AbstractKType(
    computeJavaType: (() -> Type)?,
) : KTypeBase, FlexibleTypeMarker, SimpleTypeMarker, TypeArgumentListMarker,
    DefinitelyNotNullTypeMarker {
    protected val computeJavaType =
        computeJavaType as? ReflectProperties.LazySoftVal<Type> ?: computeJavaType?.let(ReflectProperties::lazySoft)

    override val javaType: Type?
        get() = computeJavaType?.invoke()

    abstract fun isSubtypeOf(other: AbstractKType): Boolean

    abstract fun makeNullableAsSpecified(nullable: Boolean): AbstractKType

    abstract fun makeDefinitelyNotNullAsSpecified(isDefinitelyNotNull: Boolean): AbstractKType

    abstract val abbreviation: KType?

    abstract val isDefinitelyNotNullType: Boolean

    abstract val isNothingType: Boolean
    abstract val isMutableCollectionType: Boolean
    abstract val isSuspendFunctionType: Boolean
    abstract val isRawType: Boolean
    abstract fun lowerBoundIfFlexible(): AbstractKType?
    abstract fun upperBoundIfFlexible(): AbstractKType?

    override fun toString(): String =
        ReflectionObjectRenderer.renderType(this)
}
