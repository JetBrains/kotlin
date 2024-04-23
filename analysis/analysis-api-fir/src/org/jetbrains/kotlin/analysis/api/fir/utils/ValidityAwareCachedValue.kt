/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Lazy value that guaranties safe publication and checks validity on every access
 */
@JvmInline
internal value class ValidityAwareCachedValue<T>(
    private val lazyValue: Lazy<T>,
) : ReadOnlyProperty<KtLifetimeOwner, T> {
    override fun getValue(thisRef: KtLifetimeOwner, property: KProperty<*>): T {
        return thisRef.withValidityAssertion { lazyValue.value }
    }
}

@Suppress("UnusedReceiverParameter") // we need to have the KtLifetimeOwner as receiver to make sure it's called only for KtLifetimeOwner
internal fun <T> KtLifetimeOwner.cached(init: () -> T): ValidityAwareCachedValue<T> {
    return ValidityAwareCachedValue(lazy(LazyThreadSafetyMode.PUBLICATION, init))
}
