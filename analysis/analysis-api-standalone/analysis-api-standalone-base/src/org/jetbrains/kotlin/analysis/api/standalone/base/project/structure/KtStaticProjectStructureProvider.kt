/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.project.structure

import com.intellij.psi.PsiFileSystemItem
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtNotUnderContentRootModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider

/**
 * A [ProjectStructureProvider] with a static module structure.
 *
 * Static project structure providers may still create [KtNotUnderContentRootModule]s on the fly, because files which don't belong to any
 * of the pre-registered modules are by definition not part of the *static* module structure.
 */
public abstract class KtStaticProjectStructureProvider : ProjectStructureProvider() {
    /**
     * All [KtModule]s registered with the project structure provider, excluding [KtNotUnderContentRootModule]s and the built-ins module.
     *
     * [allKtModules] may be used by other services to pre-build caches based on the full module structure.
     */
    public abstract val allKtModules: List<KtModule>

    public abstract val allSourceFiles: List<PsiFileSystemItem>
}
