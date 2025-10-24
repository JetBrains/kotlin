/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.io.Serializable
import kotlin.reflect.KType
import kotlin.reflect.jvm.internal.types.SimpleKType
import kotlin.reflect.typeOf

internal object StandardKTypes {
    val ANY: KType = typeOf<Any>()
    val NULLABLE_ANY: KType = typeOf<Any?>()

    val CLONEABLE: KType = typeOf<Cloneable>()
    val SERIALIZABLE: KType = typeOf<Serializable>()

    val UNIT_RETURN_TYPE: KType = SimpleKType(
        Unit::class, arguments = emptyList(), isMarkedNullable = false, annotations = emptyList(), abbreviation = null,
        isDefinitelyNotNullType = false, isNothingType = false, isSuspendFunctionType = false, mutableCollectionClass = null,
    ) { Void.TYPE }
}
