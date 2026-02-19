/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.projectStructure

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.resolveExtensionFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

@KaImplementationDetail
class KaBaseResolveExtensionGeneratedFilesScope(val useSiteModules: List<KaModule>) : KaResolveExtensionGeneratedFilesScope() {
    override fun isSearchInModuleContent(aModule: Module): Boolean = false

    override fun isSearchInLibraries(): Boolean = false

    override fun getProject(): Project? = useSiteModules.firstOrNull()?.project

    override fun contains(file: VirtualFile): Boolean = file.resolveExtensionFileModule in useSiteModules

    override fun toString(): String =
        "Resolve Extensions Generated File Scope for [" + useSiteModules.joinToString(", ") { it.moduleDescription } + "]"
}
