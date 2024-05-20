/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.lifetime

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import kotlin.reflect.KClass

public class KaAlwaysAccessibleLifetimeToken(project: Project) : KaLifetimeToken() {
    private val modificationTracker = project.createProjectWideOutOfBlockModificationTracker()
    private val onCreatedTimeStamp = modificationTracker.modificationCount

    override fun isValid(): Boolean {
        return onCreatedTimeStamp == modificationTracker.modificationCount
    }

    override fun getInvalidationReason(): String {
        if (onCreatedTimeStamp != modificationTracker.modificationCount) return "PSI has changed since creation"
        error("Getting invalidation reason for valid validity token")
    }

    override fun isAccessible(): Boolean {
        return true
    }

    override fun getInaccessibilityReason(): String {
        error("Getting inaccessibility reason for validity token when it is accessible")
    }

    override val factory: KaLifetimeTokenFactory = KaAlwaysAccessibleLifetimeTokenFactory
}

public typealias KtAlwaysAccessibleLifetimeToken = KaAlwaysAccessibleLifetimeToken

public object KaAlwaysAccessibleLifetimeTokenFactory : KaLifetimeTokenFactory() {
    override val identifier: KClass<out KaLifetimeToken> = KaAlwaysAccessibleLifetimeToken::class

    override fun create(project: Project, modificationTracker: ModificationTracker): KaLifetimeToken =
        KaAlwaysAccessibleLifetimeToken(project)
}

public typealias KtAlwaysAccessibleLifetimeTokenFactory = KaAlwaysAccessibleLifetimeTokenFactory