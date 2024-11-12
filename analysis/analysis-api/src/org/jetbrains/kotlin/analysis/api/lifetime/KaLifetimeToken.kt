/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.lifetime

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail

public abstract class KaLifetimeToken {
    public abstract fun isValid(): Boolean
    public abstract fun getInvalidationReason(): String

    public abstract fun isAccessible(): Boolean
    public abstract fun getInaccessibilityReason(): String
}

@Suppress("NOTHING_TO_INLINE")
public inline fun KaLifetimeToken.assertIsValidAndAccessible() {
    assertIsValid()
    assertIsAccessible()
}

@OptIn(KaImplementationDetail::class)
@Suppress("NOTHING_TO_INLINE")
public inline fun KaLifetimeToken.assertIsValid() {
    if (!isValid()) {
        throw KaInvalidLifetimeOwnerAccessException("Access to invalid $this: ${getInvalidationReason()}")
    }
}

@OptIn(KaImplementationDetail::class)
@Suppress("NOTHING_TO_INLINE")
public inline fun KaLifetimeToken.assertIsAccessible() {
    if (!isAccessible()) {
        throw KaInaccessibleLifetimeOwnerAccessException("$this is inaccessible: ${getInaccessibilityReason()}")
    }
}

@KaImplementationDetail
public abstract class KaIllegalLifetimeOwnerAccessException : IllegalStateException()

@KaImplementationDetail
public class KaInvalidLifetimeOwnerAccessException(override val message: String) : KaIllegalLifetimeOwnerAccessException()

@KaImplementationDetail
public class KaInaccessibleLifetimeOwnerAccessException(override val message: String) : KaIllegalLifetimeOwnerAccessException()
