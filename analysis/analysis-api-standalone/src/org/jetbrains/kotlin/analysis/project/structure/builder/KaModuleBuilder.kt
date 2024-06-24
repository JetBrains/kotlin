/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.platform.TargetPlatform

@KtModuleBuilderDsl
public abstract class KtModuleBuilder {
    protected val directRegularDependencies: MutableList<KaModule> = mutableListOf()
    protected val directDependsOnDependencies: MutableList<KaModule> = mutableListOf()
    protected val directFriendDependencies: MutableList<KaModule> = mutableListOf()

    public fun addRegularDependency(module: KaModule) {
        directRegularDependencies.add(module)
    }

    public fun addDependsOnDependency(module: KaModule) {
        directDependsOnDependencies.add(module)
    }

    public fun addFriendDependency(module: KaModule) {
        directFriendDependencies.add(module)
    }

    public lateinit var platform: TargetPlatform

    public abstract fun build(): KaModule
}