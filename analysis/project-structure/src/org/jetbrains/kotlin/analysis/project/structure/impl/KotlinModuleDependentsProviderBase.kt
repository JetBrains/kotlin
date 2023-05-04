/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.impl

import org.jetbrains.kotlin.analysis.project.structure.KotlinModuleDependentsProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModule

public abstract class KotlinModuleDependentsProviderBase : KotlinModuleDependentsProvider() {
    override fun getTransitiveDependents(module: KtModule): Set<KtModule> = computeTransitiveDependents(module)

    protected fun computeTransitiveDependents(module: KtModule): Set<KtModule> = buildSet {
        // We could use `DFS` from utils, but this implementation has no handler overhead and is simple enough.
        fun visit(module: KtModule) {
            if (module in this) return
            add(module)
            getDirectDependents(module).forEach(::visit)
        }
        visit(module)

        // `module` should not be part of its own dependents, per the contract of `KotlinModuleDependentsProvider`.
        remove(module)
    }
}
