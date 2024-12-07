/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.lifetime

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.platform.KaCachedService
import org.jetbrains.kotlin.analysis.api.platform.permissions.KaAnalysisPermissionChecker
import kotlin.reflect.KClass

public class KotlinReadActionConfinementLifetimeToken(
    project: Project,
    private val modificationTracker: ModificationTracker,
) : KaLifetimeToken() {
    private val onCreatedTimeStamp = modificationTracker.modificationCount

    // We cache several services to avoid repeated `getService` calls in validity assertions.
    @KaCachedService
    private val permissionChecker = KaAnalysisPermissionChecker.getInstance(project)

    @KaCachedService
    private val lifetimeTracker = KaLifetimeTracker.getInstance(project)

    override fun isValid(): Boolean {
        return onCreatedTimeStamp == modificationTracker.modificationCount
    }

    @OptIn(KaImplementationDetail::class)
    override fun getInvalidationReason(): String {
        if (onCreatedTimeStamp != modificationTracker.modificationCount) {
            return if (modificationTracker is ModificationTrackerWithInvalidationReason) {
                val trackerInvalidationReason = modificationTracker.getInvalidationReason()
                    ?: error("Cannot get an invalidation reason for a ${ModificationTrackerWithInvalidationReason::class.simpleName} that's valid'")
                "Session is invalidated: $trackerInvalidationReason"
            } else {
                "Session is invalidated"
            }
        }
        error("Cannot get an invalidation reason for a valid lifetime token.")
    }

    override fun isAccessible(): Boolean {
        if (!ApplicationManager.getApplication().isReadAccessAllowed) return false
        if (!permissionChecker.isAnalysisAllowed()) return false

        return lifetimeTracker.currentToken == this
    }

    override fun getInaccessibilityReason(): String {
        if (!ApplicationManager.getApplication().isReadAccessAllowed) return "Called outside a read action."

        if (!permissionChecker.isAnalysisAllowed()) return permissionChecker.getRejectionReason()

        val currentToken = lifetimeTracker.currentToken
        if (currentToken == null) return "Called outside an `analyze` context."
        if (currentToken != this) return "Using a lifetime owner from an old `analyze` context."

        error("Cannot get an inaccessibility reason for a lifetime token when it's accessible.")
    }
}

public class KotlinReadActionConfinementLifetimeTokenFactory : KotlinLifetimeTokenFactory {
    override val identifier: KClass<out KaLifetimeToken> = KotlinReadActionConfinementLifetimeToken::class

    override fun create(project: Project, modificationTracker: ModificationTracker): KaLifetimeToken =
        KotlinReadActionConfinementLifetimeToken(project, modificationTracker)
}
