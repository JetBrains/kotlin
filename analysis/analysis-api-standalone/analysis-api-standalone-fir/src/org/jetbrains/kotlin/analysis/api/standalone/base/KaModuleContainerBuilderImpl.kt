/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneWorkaroundApi
import org.jetbrains.kotlin.analysis.api.standalone.projectStructure.StandaloneLibraryScopeConstructionMode
import org.jetbrains.kotlin.analysis.project.structure.builder.KaModuleContainer
import org.jetbrains.kotlin.analysis.project.structure.builder.KaModuleContainerBuilder
import org.jetbrains.kotlin.platform.TargetPlatform
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal class KaModuleContainerBuilderImpl(
    override val coreApplicationEnvironment: CoreApplicationEnvironment,
    override val project: Project,
) : KaModuleContainerBuilder() {
    private val allModules: MutableList<KaModule> = mutableListOf()

    override fun <M : KaModule> addModule(module: M): M {
        allModules.add(module)
        return module
    }

    @OptIn(StandaloneWorkaroundApi::class)
    override var libraryScopeConstructionMode: StandaloneLibraryScopeConstructionMode =
        StandaloneLibraryScopeConstructionMode.ParentTraversal

    override fun build(): KaModuleContainer {
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

    val moduleContainerBuilder = KaModuleContainerBuilderImpl(coreApplicationEnvironment, project).apply(init)
    return moduleContainerBuilder.build() to moduleContainerBuilder.platform
}


internal class KaModuleContainerImpl(override val allModules: List<KaModule>) : KaModuleContainer
