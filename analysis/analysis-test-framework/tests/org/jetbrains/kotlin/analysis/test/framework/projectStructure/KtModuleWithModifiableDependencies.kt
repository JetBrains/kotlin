/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaModuleBase
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.computeTransitiveDependsOnDependencies
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

abstract class KtModuleWithModifiableDependencies : KaModuleBase() {
    abstract override val project: Project

    abstract override val directRegularDependencies: MutableList<KaModule>
    abstract override val directDependsOnDependencies: MutableList<KaModule>
    abstract override val directFriendDependencies: MutableList<KaModule>

    /**
     * When dependencies are modifiable, transitive `dependsOn` dependencies must be recomputed each time as [directDependsOnDependencies]
     * may have been mutated.
     */
    override val transitiveDependsOnDependencies: List<KaModule>
        get() = computeTransitiveDependsOnDependencies(directDependsOnDependencies)
}
