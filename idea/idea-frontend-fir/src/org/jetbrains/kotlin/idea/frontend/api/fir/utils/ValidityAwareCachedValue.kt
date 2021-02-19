/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.utils

import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.tokens.assertIsValidAndAccessible
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Lazy value that guaranties safe publication and checks validity on every access
 */
class ValidityAwareCachedValue<T>(
    private val token: ValidityToken,
    init: () -> T
) : ReadOnlyProperty<Any, T> {
    private val lazyValue = lazy(LazyThreadSafetyMode.PUBLICATION, init)

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        token.assertIsValidAndAccessible()
        return lazyValue.value
    }
}

internal fun <T> ValidityTokenOwner.cached(init: () -> T) = ValidityAwareCachedValue(token, init)
