/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure

import com.intellij.openapi.project.Project

/**
 * [KotlinModuleDependentsProvider] provides dependents for a [KtModule], which are modules that depend on the [KtModule].
 *
 * [getDirectDependents] and [getTransitiveDependents] may return an empty set for [KtBuiltinsModule]s and [KtSdkModule]s even though most
 * modules depend on builtins/SDKs, because it is often not feasible to compute that set. Instead, users of [KotlinModuleDependentsProvider]
 * should keep this limitation in mind and handle it separately. For example, a global modification event should be published for builtins
 * and SDK changes.
 *
 * Implementations of this provider should ensure that results are provided in reasonable time, for example by caching results, as its
 * functions may be called frequently.
 */
public abstract class KotlinModuleDependentsProvider {
    /**
     * Returns all direct dependents of [module], excluding [module] if it depends on itself.
     */
    public abstract fun getDirectDependents(module: KtModule): Set<KtModule>

    /**
     * Returns all direct and indirect dependents of [module], excluding [module] if it depends on itself.
     */
    public abstract fun getTransitiveDependents(module: KtModule): Set<KtModule>

    public companion object {
        public fun getInstance(project: Project): KotlinModuleDependentsProvider =
            project.getService(KotlinModuleDependentsProvider::class.java)
    }
}
