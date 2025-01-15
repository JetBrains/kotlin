/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.utils.errors.withKaModuleEntry
import org.jetbrains.kotlin.analysis.api.utils.errors.withPsiEntry
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment

public class KaDanglingFileModuleImpl(
    files: List<KtFile>,
    override val contextModule: KaModule,
    override val resolutionMode: KaDanglingFileResolutionMode,
) : KaDanglingFileModule, KaModuleBase() {
    override val isCodeFragment: Boolean = files.any { it is KtCodeFragment }

    @Suppress("DEPRECATION")
    private val fileRefs = files.map { it.createSmartPointer() }

    init {
        require(contextModule != this)

        if (contextModule is KaDanglingFileModule) {
            // Only code fragments can depend on dangling files.
            // This is needed for completion, inspections and refactorings.
            @OptIn(KaImplementationDetail::class)
            requireWithAttachment(
                isCodeFragment,
                message = { "Dangling file module cannot depend on another dangling file module unless it's a code fragment" },
            ) {
                withKaModuleEntry("contextModule", contextModule)
                withEntryGroup("this") {
                    files.forEachIndexed { index, file -> withPsiEntry("file_$index", file, module = null) }
                    withEntry("resolutionMode", resolutionMode.toString())
                }
            }
        }
    }

    override val files: List<KtFile>
        get() = validFilesOrNull ?: error("Dangling file module is invalid")

    override val project: Project
        get() = contextModule.project

    override val targetPlatform: TargetPlatform
        get() = contextModule.targetPlatform

    override val baseContentScope: GlobalSearchScope
        get() {
            val virtualFiles = files.mapNotNull { it.virtualFile }
            return when {
                virtualFiles.isEmpty() -> GlobalSearchScope.EMPTY_SCOPE
                else -> GlobalSearchScope.filesScope(project, virtualFiles)
            }
        }

    override val directRegularDependencies: List<KaModule>
        get() = contextModule.directRegularDependencies

    override val directDependsOnDependencies: List<KaModule>
        get() = contextModule.directDependsOnDependencies

    override val directFriendDependencies: List<KaModule>
        get() = listOf(contextModule) + contextModule.directFriendDependencies

    override val transitiveDependsOnDependencies: List<KaModule>
        get() = contextModule.transitiveDependsOnDependencies

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other is KaDanglingFileModuleImpl) {
            val selfFiles = this.fileRefs.mapNotNull { it.element }
            val otherFiles = other.fileRefs.mapNotNull { it.element }
            return selfFiles.isNotEmpty() && selfFiles == otherFiles
                    && contextModule == other.contextModule
                    && resolutionMode == other.resolutionMode
        }

        return false
    }

    override fun hashCode(): Int {
        var result = contextModule.hashCode()
        for (fileRef in fileRefs) {
            result = 31 * result + fileRef.element.hashCode()
        }
        return result
    }

    override fun toString(): String {
        val files = validFilesOrNull
        if (files != null) {
            return files.joinToString { it.name }
        }

        return "Invalid dangling file module"
    }

    private val validFilesOrNull: List<KtFile>?
        get() {
            val result = ArrayList<KtFile>(fileRefs.size)
            for (fileRef in fileRefs) {
                val file = fileRef.element?.takeIf { it.isValid } ?: return null
                result.add(file)
            }
            return result
        }
}
