/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.ValidityTokenOwner
import java.lang.ref.WeakReference
import kotlin.reflect.KProperty

internal class LazyThreadUnsafeWeakRef<V : Any>
constructor(_createValue: () -> V, val token: ValidityToken) {
    private var createValue: (() -> V)? = _createValue
    private var weakRef: WeakReference<V>? = null

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun getValue(thisRef: Any, property: KProperty<*>): V {
        if (weakRef == null) {
            weakRef = WeakReference(createValue!!())
            createValue = null
        }
        return weakRef!!.get() ?: if (token.isValid()) {
            throw EntityWasGarbageCollectedException(property.toString())
        } else {
            error("Accessing the invalid value of $property")
        }
    }
}

internal fun <V : Any> ValidityTokenOwner.lazyThreadUnsafeWeakRef(createValue: () -> V) = LazyThreadUnsafeWeakRef(createValue, token)