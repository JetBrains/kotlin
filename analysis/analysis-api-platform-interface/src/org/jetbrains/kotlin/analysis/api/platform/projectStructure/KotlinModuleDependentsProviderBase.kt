/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

public abstract class KotlinModuleDependentsProviderBase : KotlinModuleDependentsProvider() {
    override fun getTransitiveDependents(module: KaModule): Set<KaModule> = computeTransitiveDependents(module)

    protected fun computeTransitiveDependents(module: KaModule): Set<KaModule> = buildSet {
        // We could use `DFS` from utils, but this implementation has no handler overhead and is simple enough.
        fun visit(module: KaModule) {
            if (module in this) return
            add(module)
            getDirectDependents(module).forEach(::visit)
        }
        visit(module)

        // `module` should not be part of its own dependents, per the contract of `KotlinModuleDependentsProvider`.
        remove(module)
    }
}
