/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.lifetime

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Represents a value with validity assertions.
 *
 * To create an instance of [KaLifetimeOwnerField] use the [validityAsserted] function.
 *
 * @see KaLifetimeOwner
 * @see validityAsserted
 */
@JvmInline
public value class KaLifetimeOwnerField<T>(public val value: T) : ReadOnlyProperty<KaLifetimeOwner, T> {
    public override fun getValue(thisRef: KaLifetimeOwner, property: KProperty<*>): T {
        return thisRef.withValidityAssertion { value }
    }
}

public typealias KtLifetimeOwnerField<T> = KaLifetimeOwnerField<T>