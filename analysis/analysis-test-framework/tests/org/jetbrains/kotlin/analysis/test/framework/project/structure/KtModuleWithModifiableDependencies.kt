/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import org.jetbrains.kotlin.analysis.project.structure.*

abstract class KtModuleWithModifiableDependencies {
    abstract val directRegularDependencies: MutableList<KtModule>
    abstract val directDependsOnDependencies: MutableList<KtModule>
    abstract val directFriendDependencies: MutableList<KtModule>

    /**
     * When dependencies are modifiable, transitive `dependsOn` dependencies must be recomputed each time as [directDependsOnDependencies]
     * may have been mutated.
     */
    val transitiveDependsOnDependencies: List<KtModule>
        get() = computeTransitiveDependsOnDependencies(directDependsOnDependencies)
}
