/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.lifetime

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import kotlin.reflect.KClass

public abstract class KtLifetimeToken {
    public abstract fun isValid(): Boolean
    public abstract fun getInvalidationReason(): String

    public abstract fun isAccessible(): Boolean
    public abstract fun getInaccessibilityReason(): String

    public abstract val factory: KtLifetimeTokenFactory
}

public abstract class KtLifetimeTokenFactory {
    public abstract val identifier: KClass<out KtLifetimeToken>

    public abstract fun create(project: Project, modificationTracker: ModificationTracker): KtLifetimeToken

    public open fun beforeEnteringAnalysisContext(token: KtLifetimeToken) {}
    public open fun afterLeavingAnalysisContext(token: KtLifetimeToken) {}
}


@Suppress("NOTHING_TO_INLINE")
public inline fun KtLifetimeToken.assertIsValidAndAccessible() {
    if (!isValid()) {
        throw KtInvalidLifetimeOwnerAccessException("Access to invalid $this: ${getInvalidationReason()}")
    }
    if (!isAccessible()) {
        throw KtInaccessibleLifetimeOwnerAccessException("$this is inaccessible: ${getInaccessibilityReason()}")
    }
}

public abstract class KtIllegalLifetimeOwnerAccessException : IllegalStateException()

public class KtInvalidLifetimeOwnerAccessException(override val message: String) : KtIllegalLifetimeOwnerAccessException()
public class KtInaccessibleLifetimeOwnerAccessException(override val message: String) : KtIllegalLifetimeOwnerAccessException()

