/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.project.structure.impl.KaModuleContainerImpl
import org.jetbrains.kotlin.platform.TargetPlatform
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public class KtModuleContainerBuilder(
    public val coreApplicationEnvironment: CoreApplicationEnvironment,
    public val project: Project,
) {
    private val allModules: MutableList<KaModule> = mutableListOf()

    public fun <M : KaModule> addModule(module: M): M {
        allModules.add(module)
        return module
    }

    public lateinit var platform: TargetPlatform

    public fun build(): KaModuleContainer {
        return KaModuleContainerImpl(allModules)
    }
}

@OptIn(ExperimentalContracts::class)
internal inline fun buildModuleContainer(
    coreApplicationEnvironment: CoreApplicationEnvironment,
    project: Project,
    init: KtModuleContainerBuilder.() -> Unit
): Pair<KaModuleContainer, TargetPlatform> {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    val moduleContainerBuilder = KtModuleContainerBuilder(coreApplicationEnvironment, project).apply(init)
    return moduleContainerBuilder.build() to moduleContainerBuilder.platform
}
