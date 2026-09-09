/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.standalone.projectStructure.StandaloneLibraryScopeConstructionMode
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneWorkaroundApi
import org.jetbrains.kotlin.platform.TargetPlatform

public abstract class KaModuleContainerBuilder {
    public abstract val coreApplicationEnvironment: CoreApplicationEnvironment
    public abstract val project: Project

    public abstract fun <M : KaModule> addModule(module: M): M

    public open lateinit var platform: TargetPlatform

    /**
     * The default [StandaloneLibraryScopeConstructionMode] for library modules created within this provider via [buildKtLibraryModule] or
     * [buildKtSdkModule]. Individual modules may override it.
     *
     * The option is a **workaround** for exceptional cases. See [StandaloneLibraryScopeConstructionMode] for more information.
     *
     * **Caution:** Because a library module's content scope is built eagerly when the module is created, this default must be set *before*
     * the corresponding module-building calls.
     */
    @StandaloneWorkaroundApi
    public abstract var libraryScopeConstructionMode: StandaloneLibraryScopeConstructionMode

    public abstract fun build(): KaModuleContainer
}
