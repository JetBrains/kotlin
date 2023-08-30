/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtStaticProjectStructureProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.impl.KtModuleProviderImpl
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.platform.TargetPlatform
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public class KtModuleProviderBuilder(
    public val kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment,
) {
    private val mainModules: MutableList<KtModule> = mutableListOf()

    public fun <M : KtModule> addModule(module: M): M {
        mainModules.add(module)
        return module
    }

    public lateinit var platform: TargetPlatform

    public fun build(): KtStaticProjectStructureProvider {
        return KtModuleProviderImpl(
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
): KtStaticProjectStructureProvider {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KtModuleProviderBuilder(kotlinCoreProjectEnvironment).apply(init).build()
}
