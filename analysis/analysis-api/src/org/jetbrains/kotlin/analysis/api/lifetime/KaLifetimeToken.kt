/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.lifetime

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail

/**
 * A token which is used as an anchor for determining the lifetime of a [session][org.jetbrains.kotlin.analysis.api.KaSession]'s
 * [lifetime owner][KaLifetimeOwner].
 *
 * See the documentation for [KaSession][org.jetbrains.kotlin.analysis.api.KaSession] and [KaLifetimeOwner] to find out more about lifetime
 * management.
 */
public abstract class KaLifetimeToken {
    /**
     * Whether the lifetime token is valid, i.e. the underlying information is still up-to-date. Invalidation most often occurs after
     * modification, such as source code or project structure changes.
     *
     * Invalidity should be stable: once the lifetime token is invalid, it should not become valid again.
     */
    public abstract fun isValid(): Boolean

    /**
     * Returns the reason why [isValid] is `false`. If the lifetime token is not invalid, the function throws an exception.
     */
    public abstract fun getInvalidationReason(): String

    /**
     * Whether the lifetime token is currently accessible. Depending on the Analysis API platform, there are certain requirements to access
     * the Analysis API. For example, in IntelliJ, the Analysis API must be invoked with read access.
     */
    public abstract fun isAccessible(): Boolean

    /**
     * Returns the reason why [isAccessible] is `false`. If the lifetime token is not inaccessible, the function throws an exception.
     */
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
