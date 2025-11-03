/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryFallbackDependenciesModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.platform.TargetPlatform
import java.nio.file.Path

open class KaLibraryModuleImpl(
    override val libraryName: String,
    override val targetPlatform: TargetPlatform,
    override val baseContentScope: GlobalSearchScope,
    override val project: Project,
    @Deprecated("Use `binaryVirtualFiles` instead. See KT-72676", replaceWith = ReplaceWith("binaryVirtualFiles"))
    override val binaryRoots: Collection<Path>,
    override val binaryVirtualFiles: Collection<VirtualFile>,
    override var librarySources: KaLibrarySourceModule?,
    override val isSdk: Boolean,
) : KtModuleWithModifiableDependencies(), KaLibraryModule {
    override val directRegularDependencies: MutableList<KaModule> = mutableListOf()
    override val directDependsOnDependencies: MutableList<KaModule> = mutableListOf()
    override val directFriendDependencies: MutableList<KaModule> = mutableListOf()

    override val areDependenciesComplete: Boolean
        get() = hasFallbackDependencies

    private val hasFallbackDependencies: Boolean
        get() = directRegularDependencies.any { it is KaLibraryFallbackDependenciesModule }

    override fun toString(): String = libraryName
}
