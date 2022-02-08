/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.ValidityTokenOwner
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import java.lang.ref.WeakReference
import kotlin.reflect.KProperty

class ReadOnlyWeakRef<V : Any>
@Deprecated("Consider using ValidityTokenOwner.weakRef instead")
constructor(value: V, val token: ValidityToken) {
    val weakRef = WeakReference(value)

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun getValue(thisRef: Any, property: KProperty<*>): V =
        weakRef.get() ?: if (token.isValid()) {
            throw EntityWasGarbageCollectedException(property.toString())
        } else {
            error("Accessing the invalid value of $property")
        }
}

@Suppress("NOTHING_TO_INLINE", "DEPRECATION")
internal inline fun <V : Any> ValidityTokenOwner.weakRef(value: V) = ReadOnlyWeakRef(value, token)

@Suppress("DEPRECATION")
internal inline fun <V : Any> ValidityTokenOwner.weakRef(value: () -> V) = ReadOnlyWeakRef(value(), token)
