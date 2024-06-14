/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.lifetime

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.platform.KaEngineService

/**
 * [KaLifetimeTracker] is an *engine service* which tracks the current [KaLifetimeToken].
 *
 * It can be used in the implementation of custom lifetime tokens to check that the accessed token is in scope.
 */
public interface KaLifetimeTracker : KaEngineService {
    /**
     * Returns the [KaLifetimeToken] for the currently active analysis, or `null` if no analysis is in progress.
     */
    public val currentToken: KaLifetimeToken?

    public companion object {
        public fun getInstance(project: Project): KaLifetimeTracker =
            project.getService(KaLifetimeTracker::class.java)
    }
}
