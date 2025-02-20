/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.KotlinStaticProjectStructureProvider
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
