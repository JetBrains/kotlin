/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.impl.KtModuleProviderImpl
import org.jetbrains.kotlin.platform.TargetPlatform
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtStaticProjectStructureProvider

@KtModuleBuilderDsl
public class KtModuleProviderBuilder {
    private val mainModules: MutableList<KtModule> = mutableListOf()

    public fun addModule(module: KtModule) {
        mainModules.add(module)
    }

    public lateinit var platform: TargetPlatform
    public lateinit var project: Project

    public fun build(): KtStaticProjectStructureProvider {
        return KtModuleProviderImpl(
            platform,
            project,
            mainModules,
        )
    }
}

@OptIn(ExperimentalContracts::class)
public inline fun buildProjectStructureProvider(init: KtModuleProviderBuilder.() -> Unit): KtStaticProjectStructureProvider {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KtModuleProviderBuilder().apply(init).build()
}
