/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.platform.TargetPlatform

@KtModuleBuilderDsl
public abstract class KtModuleBuilder {
    protected val directRegularDependencies: MutableList<KtModule> = mutableListOf()
    protected val directDependsOnDependencies: MutableList<KtModule> = mutableListOf()
    protected val directFriendDependencies: MutableList<KtModule> = mutableListOf()

    public fun addRegularDependency(module: KtModule) {
        directRegularDependencies.add(module)
    }

    public fun addDependsOnDependency(module: KtModule) {
        directDependsOnDependencies.add(module)
    }

    public fun addFriendDependency(module: KtModule) {
        directFriendDependencies.add(module)
    }

    public lateinit var platform: TargetPlatform

    public abstract fun build(): KtModule
}