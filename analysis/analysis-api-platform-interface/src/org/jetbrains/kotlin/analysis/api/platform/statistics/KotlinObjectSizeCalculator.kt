/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.statistics

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.KotlinOptionalPlatformComponent

/**
 * Provides endpoints for calculating the runtime size of objects.
 *
 * *This interface is not intended to be used in production. It should only be used for statistics purposes.*
 */
public interface KotlinObjectSizeCalculator : KotlinOptionalPlatformComponent {
    /**
     * Calculates the shallow runtime size of the given [value] in bytes.
     *
     * *This function is not intended to be used in production. It should only be used for statistics purposes.*
     */
    public fun shallowSize(value: Any): Long

    public companion object {
        public fun getInstance(project: Project): KotlinObjectSizeCalculator? = project.serviceOrNull()
    }
}
