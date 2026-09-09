/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.builder

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneWorkaroundApi
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.KaLibraryModuleImpl
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.project.structure.builder.KtSdkModuleBuilder
import java.nio.file.Path

internal class KtLibraryModuleBuilderImpl(
    private val coreApplicationEnvironment: CoreApplicationEnvironment,
    private val project: Project,
    private val isSdk: Boolean,
) : KtSdkModuleBuilder() {

    override fun addBinaryRootsFromJdkHome(jdkHome: Path, isJre: Boolean) {
        val jdkRoots = LibraryUtils.findClassesFromJdkHome(jdkHome, isJre)
        addBinaryRoots(jdkRoots)
    }

    @OptIn(StandaloneWorkaroundApi::class)
    override fun build(): KaLibraryModule {
        val binaryRoots = getBinaryRoots()
        val binaryVirtualFiles = getBinaryVirtualFiles()

        val contentScope = contentScope
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
            contentScope,
            platform,
            project,
            binaryRoots,
            binaryVirtualFiles,
            libraryName,
            librarySources,
            isSdk,
        )
    }
}
