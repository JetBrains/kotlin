/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mobile.gradle

import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.mobile.isAndroid
import org.jetbrains.kotlin.android.model.AndroidModuleInfoProvider
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.GradleUtil
import java.io.File

class AndroidModuleInfoProviderImpl(override val module: Module) : AndroidModuleInfoProvider {
    override fun isAndroidModule(): Boolean = module.isAndroid
    override fun isGradleModule(): Boolean = ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module)

    override fun getActiveSourceProviders(): List<AndroidModuleInfoProvider.SourceProviderMirror> =
        findResDirectories()
            .groupBy { it.parentFile.name }
            .map { Mirror(it.key, it.value) }

    @Suppress("OverridingDeprecatedMember")
    override fun getMainAndFlavorSourceProviders(): List<AndroidModuleInfoProvider.SourceProviderMirror> =
        getActiveSourceProviders()

    private fun findResDirectories(): List<File> {
        @Suppress("UnstableApiUsage")
        val moduleNode = GradleUtil.findGradleModuleData(module)
        return ExternalSystemApiUtil.findAllRecursively(moduleNode, ProjectKeys.CONTENT_ROOT)
            .flatMap { it.data.getPaths(ExternalSystemSourceType.RESOURCE) }
            .mapNotNull { sourceRoot ->
                File(sourceRoot.path).takeIf { it.name == "res" }
            }
    }

    override fun getApplicationPackage(): String? = "main"

    override fun getAllResourceDirectories(): List<VirtualFile> =
        emptyList() // unused method

    private class Mirror(
        override val name: String,
        val resDirectoriesIO: List<File>
    ) : AndroidModuleInfoProvider.SourceProviderMirror {
        override val resDirectories: Collection<VirtualFile>
            get() = resDirectoriesIO.mapNotNull { VfsUtil.findFileByIoFile(it, false) }
    }
}