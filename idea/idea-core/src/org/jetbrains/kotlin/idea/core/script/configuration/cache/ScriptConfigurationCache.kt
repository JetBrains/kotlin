/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.cache

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper

interface CachedConfigurationInputs {
    fun isUpToDate(project: Project, file: VirtualFile, ktFile: KtFile? = null): Boolean

    object OutOfDate : CachedConfigurationInputs {
        override fun isUpToDate(project: Project, file: VirtualFile, ktFile: KtFile?): Boolean = false
    }

    data class PsiModificationStamp(val modificationStamp: Long) : CachedConfigurationInputs {
        override fun isUpToDate(project: Project, file: VirtualFile, ktFile: KtFile?): Boolean =
            ktFile?.modificationStamp == modificationStamp
    }
}

data class CachedConfigurationSnapshot(
    val inputs: CachedConfigurationInputs,
    val configuration: ScriptCompilationConfigurationWrapper
)

interface ScriptConfigurationCache {
    operator fun get(file: VirtualFile): CachedConfigurationSnapshot?
    operator fun set(file: VirtualFile, configurationSnapshot: CachedConfigurationSnapshot)
    fun markOutOfDate(file: VirtualFile)

    fun all(): Collection<Pair<VirtualFile, ScriptCompilationConfigurationWrapper>>
}