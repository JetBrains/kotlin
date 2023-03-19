/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.assertIsValidAndAccessible
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Lazy value that guaranties safe publication and checks validity on every access
 */
@JvmInline
value class ValidityAwareCachedValue<T>(
    private val lazyValue: Lazy<T>,
) : ReadOnlyProperty<Any, T> {

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        require(thisRef is KtLifetimeOwner)
        thisRef.token.assertIsValidAndAccessible()
        return lazyValue.value
    }
}

@Suppress("UnusedReceiverParameter") // we need to have the KtLifetimeOwner as receiver to make sure it's called only for KtLifetimeOwner
internal fun <T> KtLifetimeOwner.cached(init: () -> T) = ValidityAwareCachedValue(lazy(LazyThreadSafetyMode.PUBLICATION, init))
