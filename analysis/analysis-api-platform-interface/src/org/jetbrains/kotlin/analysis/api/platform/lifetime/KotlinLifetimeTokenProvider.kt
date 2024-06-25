/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.lifetime

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformComponent

public abstract class KotlinLifetimeTokenProvider : KotlinPlatformComponent {
    public abstract fun getLifetimeTokenFactory(): KaLifetimeTokenFactory

    public companion object {
        public fun getService(project: Project): KotlinLifetimeTokenProvider =
            project.getService(KotlinLifetimeTokenProvider::class.java)
    }
}
