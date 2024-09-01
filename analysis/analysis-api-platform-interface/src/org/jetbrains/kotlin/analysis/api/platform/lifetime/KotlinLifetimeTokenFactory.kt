/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.lifetime

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformComponent
import kotlin.reflect.KClass

public interface KotlinLifetimeTokenFactory : KotlinPlatformComponent {
    public val identifier: KClass<out KaLifetimeToken>

    /**
     * Creates a [KaLifetimeToken] for a specific analysis session represented by a [modificationTracker].
     *
     * @param modificationTracker A modification tracker which tracks the validity of the analysis session's content.
     */
    public fun create(project: Project, modificationTracker: ModificationTracker): KaLifetimeToken

    public companion object {
        public fun getInstance(project: Project): KotlinLifetimeTokenFactory = project.service()
    }
}
