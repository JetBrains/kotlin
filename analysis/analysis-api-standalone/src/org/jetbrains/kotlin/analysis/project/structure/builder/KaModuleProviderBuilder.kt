/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.KotlinStaticProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.project.structure.impl.KotlinStandaloneProjectStructureProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.platform.TargetPlatform
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public class KtModuleProviderBuilder(
    public val kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment,
) {
    private val mainModules: MutableList<KaModule> = mutableListOf()

    public fun <M : KaModule> addModule(module: M): M {
        mainModules.add(module)
        return module
    }

    public lateinit var platform: TargetPlatform

    public fun build(): KotlinStaticProjectStructureProvider {
        return KotlinStandaloneProjectStructureProvider(
            platform,
            kotlinCoreProjectEnvironment.project,
            mainModules,
        )
    }
}

@OptIn(ExperimentalContracts::class)
internal inline fun buildProjectStructureProvider(
    kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment,
    init: KtModuleProviderBuilder.() -> Unit,
): KotlinStaticProjectStructureProvider {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KtModuleProviderBuilder(kotlinCoreProjectEnvironment).apply(init).build()
}
