/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.references.utils

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project

interface KotlinKDocResolutionStrategyProviderService : Disposable {
    fun shouldUseExperimentalStrategy(): Boolean

    companion object {
        fun getService(project: Project): KotlinKDocResolutionStrategyProviderService? =
            project.serviceOrNull()
    }
}