/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.lifetime

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.platform.modification.createProjectWideOutOfBlockModificationTracker
import kotlin.reflect.KClass

public class KotlinAlwaysAccessibleLifetimeToken(project: Project) : KaLifetimeToken() {
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
}

public class KotlinAlwaysAccessibleLifetimeTokenFactory : KotlinLifetimeTokenFactory {
    override val identifier: KClass<out KaLifetimeToken> = KotlinAlwaysAccessibleLifetimeToken::class

    override fun create(project: Project, modificationTracker: ModificationTracker): KaLifetimeToken =
        KotlinAlwaysAccessibleLifetimeToken(project)
}
