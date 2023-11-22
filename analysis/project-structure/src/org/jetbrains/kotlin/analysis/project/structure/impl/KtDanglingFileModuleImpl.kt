/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.impl

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.project.structure.DanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtDanglingFileModule
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import java.util.Objects

public class KtDanglingFileModuleImpl(
    file: KtFile,
    override val contextModule: KtModule,
    override val resolutionMode: DanglingFileResolutionMode
) : KtDanglingFileModule {
    override val isCodeFragment: Boolean = file is KtCodeFragment

    private val fileRef = file.createSmartPointer()

    init {
        require(contextModule != this)

        if (contextModule is KtDanglingFileModule) {
            // Only code fragments can depend on dangling files.
            // This is needed for completion, inspections and refactorings.
            require(file is KtCodeFragment)
        }
    }

    override val file: KtFile
        get() = fileRef.element?.takeIf { it.isValid } ?: error("Dangling file module is invalid")

    override val project: Project
        get() = contextModule.project

    override val platform: TargetPlatform
        get() = contextModule.platform

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = contextModule.analyzerServices

    override val contentScope: GlobalSearchScope
        get() = GlobalSearchScope.fileScope(file)

    override val directRegularDependencies: List<KtModule>
        get() = contextModule.directRegularDependencies

    override val directDependsOnDependencies: List<KtModule>
        get() = contextModule.directDependsOnDependencies

    override val directFriendDependencies: List<KtModule>
        get() = listOf(contextModule) + contextModule.directFriendDependencies

    override val transitiveDependsOnDependencies: List<KtModule>
        get() = contextModule.transitiveDependsOnDependencies

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other is KtDanglingFileModuleImpl) {
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
}