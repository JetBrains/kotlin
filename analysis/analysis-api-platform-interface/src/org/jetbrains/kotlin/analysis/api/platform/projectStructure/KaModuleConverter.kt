/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.platform.KotlinOptionalPlatformComponent
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule

/**
 * A service for converting [KaModule] to [Module].
 */
@KaIdeApi
public interface KaModuleConverter : KotlinOptionalPlatformComponent {
    /**
     * Returns [Module] corresponding to [module] or `null` if not found.
     *
     * Only supports [KaSourceModule]s. When [module] is not a [KaSourceModule], returns `null`.
     */
    public fun asOpenApiModule(module: KaModule): Module?

    @KaIdeApi
    public companion object {
        public fun getInstance(): KaModuleConverter? = ApplicationManager.getApplication().serviceOrNull()
    }
}
