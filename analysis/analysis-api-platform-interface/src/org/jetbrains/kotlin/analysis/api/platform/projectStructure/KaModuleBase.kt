/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

public abstract class KaModuleBase : KaModule {
    override val contentScope: GlobalSearchScope by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KaContentScopeProvider.getInstance(project).getRefinedContentScope(this)
    }

    override val transitiveDependsOnDependencies: List<KaModule> by lazy {
        computeTransitiveDependsOnDependencies(directDependsOnDependencies)
    }
}