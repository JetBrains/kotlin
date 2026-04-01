/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.KotlinOptionalPlatformComponent
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * [KotlinModuleInformationProvider] offers additional, secondary information about [KaModule]s.
 *
 * The information provided by this platform component is secondary in nature, meaning there isn't a compelling reason to pollute the API of
 * [KaModule] with such properties.
 */
@KaPlatformInterface
public interface KotlinModuleInformationProvider : KotlinOptionalPlatformComponent {
    /**
     * Whether [module] is empty, meaning it has no content.
     *
     * This function is used internally as an optimization to avoid creating dependency sessions for empty [KaModule]s. An empty [KaModule]
     * can still be used as the use-site module of [analyze][org.jetbrains.kotlin.analysis.api.analyze].
     *
     * @return `true` if the module is empty, `false` if it has content, or `null` if it cannot be determined.
     */
    public fun isEmpty(module: KaModule): Boolean?

    @KaPlatformInterface
    public companion object {
        public fun getInstance(project: Project): KotlinModuleInformationProvider? = project.serviceOrNull()
    }
}
