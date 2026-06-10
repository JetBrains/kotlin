/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.KotlinStaticProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneLibraryScopeConstructionMode
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.project.structure.impl.KotlinStandaloneProjectStructureProvider
import org.jetbrains.kotlin.platform.TargetPlatform
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public class KtModuleProviderBuilder(
    public val coreApplicationEnvironment: CoreApplicationEnvironment,
    public val project: Project,
) {
    private val mainModules: MutableList<KaModule> = mutableListOf()

    public fun <M : KaModule> addModule(module: M): M {
        mainModules.add(module)
        return module
    }

    public lateinit var platform: TargetPlatform

    /**
     * The default [StandaloneLibraryScopeConstructionMode] for library modules created within this provider via [buildKtLibraryModule] or
     * [buildKtSdkModule]. Individual modules may override it.
     *
     * **Caution:** Because a library module's content scope is built eagerly when the module is created, this default must be set *before*
     * the corresponding module-building calls.
     */
    public var libraryScopeConstructionMode: StandaloneLibraryScopeConstructionMode =
        StandaloneLibraryScopeConstructionMode.ParentTraversal

    public fun build(): KotlinStaticProjectStructureProvider {
        return KotlinStandaloneProjectStructureProvider(platform, project, mainModules)
    }
}

@OptIn(ExperimentalContracts::class)
internal inline fun buildProjectStructureProvider(
    coreApplicationEnvironment: CoreApplicationEnvironment,
    project: Project,
    init: KtModuleProviderBuilder.() -> Unit,
): KotlinStaticProjectStructureProvider {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KtModuleProviderBuilder(coreApplicationEnvironment, project).apply(init).build()
}
