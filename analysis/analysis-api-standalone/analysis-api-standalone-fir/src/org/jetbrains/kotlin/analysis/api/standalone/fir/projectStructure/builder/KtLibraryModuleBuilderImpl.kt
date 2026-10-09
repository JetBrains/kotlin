/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.projectStructure.builder

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneWorkaroundApi
import org.jetbrains.kotlin.analysis.api.standalone.fir.projectStructure.KaLibraryModuleImpl
import org.jetbrains.kotlin.analysis.api.standalone.fir.projectStructure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.api.standalone.projectStructure.StandaloneLibraryScopeConstructionMode
import org.jetbrains.kotlin.analysis.project.structure.builder.KtLibraryModuleBuilder
import org.jetbrains.kotlin.platform.TargetPlatform
import java.nio.file.Path

internal class KtLibraryModuleBuilderImpl(
    private val coreApplicationEnvironment: CoreApplicationEnvironment,
    private val project: Project,
) : KtLibraryModuleBuilder() {

    @OptIn(StandaloneWorkaroundApi::class)
    override fun build(): KaLibraryModule = buildLibraryModule(
        directRegularDependencies,
        directDependsOnDependencies,
        directFriendDependencies,
        getBinaryRoots(),
        getBinaryVirtualFiles(),
        contentScope,
        libraryScopeConstructionMode,
        platform,
        libraryName,
        librarySources,
        isSdk = false,
        coreApplicationEnvironment,
        project,
    )
}

@OptIn(StandaloneWorkaroundApi::class)
internal fun buildLibraryModule(
    directRegularDependencies: List<KaModule>,
    directDependsOnDependencies: List<KaModule>,
    directFriendDependencies: List<KaModule>,
    binaryRoots: List<Path>,
    binaryVirtualFiles: List<VirtualFile>,
    contentScope: GlobalSearchScope?,
    libraryScopeConstructionMode: StandaloneLibraryScopeConstructionMode,
    platform: TargetPlatform,
    libraryName: String,
    librarySources: KaLibrarySourceModule?,
    isSdk: Boolean,
    coreApplicationEnvironment: CoreApplicationEnvironment,
    project: Project,
): KaLibraryModule {
    val scope = contentScope
        ?: StandaloneProjectFactory.createLibraryModuleSearchScope(
            binaryRoots,
            binaryVirtualFiles,
            libraryScopeConstructionMode,
            coreApplicationEnvironment,
            project,
        )

    return KaLibraryModuleImpl(
        directRegularDependencies,
        directDependsOnDependencies,
        directFriendDependencies,
        scope,
        platform,
        project,
        binaryRoots,
        binaryVirtualFiles,
        libraryName,
        librarySources,
        isSdk,
    )
}
