/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import java.util.Objects

public class KaDanglingFileModuleImpl(
    file: KtFile,
    override val contextModule: KaModule,
    override val resolutionMode: KaDanglingFileResolutionMode,
) : KaDanglingFileModule {
    override val isCodeFragment: Boolean = file is KtCodeFragment

    @Suppress("DEPRECATION")
    private val fileRef = file.createSmartPointer()

    init {
        require(contextModule != this)

        if (contextModule is KaDanglingFileModule) {
            // Only code fragments can depend on dangling files.
            // This is needed for completion, inspections and refactorings.
            require(file is KtCodeFragment)
        }
    }

    override val file: KtFile
        get() = validFileOrNull ?: error("Dangling file module is invalid")

    override val project: Project
        get() = contextModule.project

    override val platform: TargetPlatform
        get() = contextModule.platform

    override val contentScope: GlobalSearchScope
        get() = GlobalSearchScope.fileScope(file)

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
            val selfFile = this.fileRef.element
            val otherFile = other.fileRef.element
            return selfFile != null && selfFile == otherFile
                    && contextModule == other.contextModule
                    && resolutionMode == other.resolutionMode
        }

        return false
    }

    override fun hashCode(): Int {
        return Objects.hash(fileRef.element, contextModule)
    }

    override fun toString(): String = validFileOrNull?.name ?: "Invalid dangling file module"

    private val validFileOrNull: KtFile?
        get() = fileRef.element?.takeIf { it.isValid }
}
