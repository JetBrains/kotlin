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
import org.jetbrains.kotlin.analysis.project.structure.impl.KaModuleContainerImpl
import org.jetbrains.kotlin.platform.TargetPlatform
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public class KaModuleContainerBuilder(
    public val coreApplicationEnvironment: CoreApplicationEnvironment,
    public val project: Project,
) {
    private val allModules: MutableList<KaModule> = mutableListOf()

    public fun <M : KaModule> addModule(module: M): M {
        allModules.add(module)
        return module
    }

    public lateinit var platform: TargetPlatform

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
    public var libraryScopeConstructionMode: StandaloneLibraryScopeConstructionMode =
        StandaloneLibraryScopeConstructionMode.ParentTraversal

    public fun build(): KaModuleContainer {
        return KaModuleContainerImpl(allModules)
    }
}

@OptIn(ExperimentalContracts::class)
internal inline fun buildModuleContainer(
    coreApplicationEnvironment: CoreApplicationEnvironment,
    project: Project,
    init: KaModuleContainerBuilder.() -> Unit
): Pair<KaModuleContainer, TargetPlatform> {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    val moduleContainerBuilder = KaModuleContainerBuilder(coreApplicationEnvironment, project).apply(init)
    return moduleContainerBuilder.build() to moduleContainerBuilder.platform
}
