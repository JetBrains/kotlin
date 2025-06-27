/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.io.Serializable
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.jvm.internal.types.AbstractKType
import kotlin.reflect.jvm.internal.types.FlexibleKType
import kotlin.reflect.typeOf

internal object StandardKTypes {
    val ANY: KType = typeOf<Any>()
    val NULLABLE_ANY: KType = typeOf<Any?>()
    val FLEXIBLE_ANY_INVARIANT: KTypeProjection = KTypeProjection.invariant(
        FlexibleKType.create(ANY as AbstractKType, NULLABLE_ANY as AbstractKType, isRawType = false)
    )

    val CLONEABLE: KType = typeOf<Cloneable>()
    val SERIALIZABLE: KType = typeOf<Serializable>()
}
