/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure

import com.intellij.psi.PsiFileSystemItem
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaNotUnderContentRootModule
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProviderBase

/**
 * A [KotlinProjectStructureProvider] with a static module structure.
 *
 * Static project structure providers may still create [KaNotUnderContentRootModule]s on the fly, because files which don't belong to any
 * of the pre-registered modules are by definition not part of the *static* module structure.
 */
public abstract class KotlinStaticProjectStructureProvider : KotlinProjectStructureProviderBase() {
    /**
     * All [KaModule]s registered with the project structure provider, excluding [KaNotUnderContentRootModule]s and the built-ins module.
     *
     * [allModules] may be used by other services to pre-build caches based on the full module structure.
     */
    public abstract val allModules: List<KaModule>

    public abstract val allSourceFiles: List<PsiFileSystemItem>
}
