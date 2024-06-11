/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformComponent
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * [KotlinModuleDependentsProvider] provides dependents for a [KaModule], which are modules that depend on the [KaModule].
 *
 * [getDirectDependents] and [getTransitiveDependents] may return an empty set for
 * [KaBuiltinsModule][org.jetbrains.kotlin.analysis.api.projectStructure.KaBuiltinsModule]s and
 * [KaSdkModule][org.jetbrains.kotlin.analysis.api.projectStructure.KaSdkModule]s even though most modules depend on builtins/SDKs, because
 * it is often not feasible to compute that set. Instead, users of [KotlinModuleDependentsProvider] should keep this limitation in mind and
 * handle it separately. For example, a global modification event should be published for builtins and SDK changes.
 *
 * An empty set is also returned for [KaDanglingFileModule][org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule]s.
 * Additionally, dangling file modules are never included in the dependents of their context modules. This is because dangling files are
 * created ad-hoc, and it's not economical to keep track of them.
 *
 * Implementations of this provider should ensure that results are provided in reasonable time, for example by caching results, as its
 * functions may be called frequently.
 */
public abstract class KotlinModuleDependentsProvider : KotlinPlatformComponent {
    /**
     * Returns all direct dependents of [module], excluding [module] if it depends on itself.
     */
    public abstract fun getDirectDependents(module: KaModule): Set<KaModule>

    /**
     * Returns all direct and indirect dependents of [module], excluding [module] if it depends on itself.
     */
    public abstract fun getTransitiveDependents(module: KaModule): Set<KaModule>

    /**
     * Returns all refinement/depends-on dependents of [module], excluding [module] itself. The result is transitive because refinement
     * dependencies are implicitly transitive.
     */
    public abstract fun getRefinementDependents(module: KaModule): Set<KaModule>

    public companion object {
        public fun getInstance(project: Project): KotlinModuleDependentsProvider =
            project.getService(KotlinModuleDependentsProvider::class.java)
    }
}
