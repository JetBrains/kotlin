/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.lifetime

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import kotlin.reflect.KClass

public abstract class KaLifetimeToken {
    public abstract val factory: KaLifetimeTokenFactory

    public abstract fun isValid(): Boolean
    public abstract fun getInvalidationReason(): String

    public abstract fun isAccessible(): Boolean
    public abstract fun getInaccessibilityReason(): String
}

public typealias KtLifetimeToken = KaLifetimeToken

public abstract class KaLifetimeTokenFactory {
    public abstract val identifier: KClass<out KaLifetimeToken>

    public abstract fun create(project: Project, modificationTracker: ModificationTracker): KaLifetimeToken
}

public typealias KtLifetimeTokenFactory = KaLifetimeTokenFactory

@Suppress("NOTHING_TO_INLINE")
public inline fun KaLifetimeToken.assertIsValidAndAccessible() {
    if (!isValid()) {
        throw KaInvalidLifetimeOwnerAccessException("Access to invalid $this: ${getInvalidationReason()}")
    }
    if (!isAccessible()) {
        throw KaInaccessibleLifetimeOwnerAccessException("$this is inaccessible: ${getInaccessibilityReason()}")
    }
}

public abstract class KaIllegalLifetimeOwnerAccessException : IllegalStateException()

public typealias KtIllegalLifetimeOwnerAccessException = KaIllegalLifetimeOwnerAccessException

public class KaInvalidLifetimeOwnerAccessException(override val message: String) : KaIllegalLifetimeOwnerAccessException()

public typealias KtInvalidLifetimeOwnerAccessException = KaInvalidLifetimeOwnerAccessException

public class KaInaccessibleLifetimeOwnerAccessException(override val message: String) : KaIllegalLifetimeOwnerAccessException()

public typealias KtInaccessibleLifetimeOwnerAccessException = KaInaccessibleLifetimeOwnerAccessException
