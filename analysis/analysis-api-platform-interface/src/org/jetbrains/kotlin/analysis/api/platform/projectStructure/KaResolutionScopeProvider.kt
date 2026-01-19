/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.KaEngineService
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * Provides a resolution scope for the given [KaModule].
 *
 * A resolution scope of [KaModule] covers files that should be considered when resolving references in this module.
 * A resolution scope is normally a union of [KaModule.contentScope]s of the given module and its direct dependencies.
 */
@KaPlatformInterface
public interface KaResolutionScopeProvider : KaEngineService {
    /**
     * Returns a [KaResolutionScope] which covers the resolvable content of [module].
     */
    public fun getResolutionScope(module: KaModule): KaResolutionScope

    @KaPlatformInterface
    public companion object {
        public fun getInstance(project: Project): KaResolutionScopeProvider = project.service()
    }
}
