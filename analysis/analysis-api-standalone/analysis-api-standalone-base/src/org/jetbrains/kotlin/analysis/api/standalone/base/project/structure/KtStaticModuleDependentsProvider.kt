/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.project.structure

import com.intellij.util.containers.ContainerUtil.createConcurrentSoftMap
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.allDirectDependencies
import org.jetbrains.kotlin.analysis.project.structure.impl.KotlinModuleDependentsProviderBase

class KtStaticModuleDependentsProvider(private val modules: List<KtModule>) : KotlinModuleDependentsProviderBase() {
    private val directDependentsByKtModule: Map<KtModule, Set<KtModule>> by lazy {
        // Direct dependencies should be computed lazily, because the built-ins module will be reachable via module dependencies. Getting
        // the built-ins module relies on the built-ins session, which may depend on services that are registered after
        // `KtStaticModuleDependentsProvider`.
        buildMap<KtModule, MutableSet<KtModule>> {
            for (module in modules) {
                for (dependency in module.allDirectDependencies()) {
                    // `module` should not be part of its own dependents, per the contract of `KotlinModuleDependentsProvider`.
                    if (dependency == module) continue

                    val dependents = computeIfAbsent(dependency) { mutableSetOf<KtModule>() }
                    dependents.add(module)
                }
            }
        }
    }

    private val transitiveDependentsByKtModule = createConcurrentSoftMap<KtModule, Set<KtModule>>()

    override fun getDirectDependents(module: KtModule): Set<KtModule> = directDependentsByKtModule[module] ?: emptySet()

    override fun getTransitiveDependents(module: KtModule): Set<KtModule> =
        transitiveDependentsByKtModule.computeIfAbsent(module) { computeTransitiveDependents(it) }
}
