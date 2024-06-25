/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.lifetime

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import kotlin.reflect.KClass

public interface KaLifetimeTokenFactory {
    public val identifier: KClass<out KaLifetimeToken>

    public fun create(project: Project, modificationTracker: ModificationTracker): KaLifetimeToken
}
