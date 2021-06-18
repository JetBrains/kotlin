/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.tokens

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.fir.low.level.api.api.createProjectWideOutOfBlockModificationTracker
import kotlin.reflect.KClass

class AlwaysAccessibleValidityToken(project: Project) : ValidityToken() {
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

object AlwaysAccessibleValidityTokenFactory : ValidityTokenFactory() {
    override val identifier: KClass<out ValidityToken> = AlwaysAccessibleValidityToken::class

    override fun create(project: Project): ValidityToken =
        AlwaysAccessibleValidityToken(project)
}
