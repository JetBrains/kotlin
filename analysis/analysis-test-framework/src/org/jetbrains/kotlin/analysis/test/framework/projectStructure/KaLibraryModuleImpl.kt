/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.platform.TargetPlatform
import java.nio.file.Path

class KaLibraryModuleImpl(
    override val libraryName: String,
    override val targetPlatform: TargetPlatform,
    override val contentScope: GlobalSearchScope,
    override val project: Project,
    override val binaryRoots: Collection<Path>,
    override var librarySources: KaLibrarySourceModule?,
    override val isSdk: Boolean,
) : KtModuleWithModifiableDependencies(), KaLibraryModule {
    override val directRegularDependencies: MutableList<KaModule> = mutableListOf()
    override val directDependsOnDependencies: MutableList<KaModule> = mutableListOf()
    override val directFriendDependencies: MutableList<KaModule> = mutableListOf()

    override val binaryVirtualFiles: Collection<VirtualFile> = emptyList()

    override fun toString(): String = libraryName
}
