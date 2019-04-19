/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.jetbrains.cidr.lang.CustomHeaderProvider
import com.jetbrains.cidr.lang.CustomTargetHeaderSerializationHelper
import com.jetbrains.cidr.lang.OCIncludeHelpers.adjustHeaderName
import com.jetbrains.cidr.lang.OCLog
import com.jetbrains.cidr.lang.preprocessor.OCResolveRootAndConfiguration
import com.jetbrains.cidr.xcode.frameworks.LocalFramework
import com.jetbrains.cidr.xcode.model.PBXTarget
import com.jetbrains.cidr.xcode.model.XCBuildConfiguration
import com.jetbrains.cidr.xcode.model.XcodeMetaData
import org.jetbrains.kotlin.idea.debugger.readAction


class KonanFrameworkHeaderProvider : CustomHeaderProvider() {
    init {
        registerProvider(HeaderSearchStage.BEFORE_LIBRARIES) { headerName, configuration ->
            val buildConfiguration = XcodeMetaData.getBuildConfigurationFor(configuration)
            OCLog.LOG.assertTrue(buildConfiguration != null)
            return@registerProvider getHeader(headerName, buildConfiguration!!)
        }
    }

    override fun accepts(configuration: OCResolveRootAndConfiguration?): Boolean {
        return configuration?.configuration?.project?.isKonanProject() == true
    }

    override fun provideSerializationPath(virtualFile: VirtualFile): String? {
        assert(ApplicationManager.getApplication().isReadAccessAllowed)

        if (virtualFile !is KonanBridgeVirtualFile || !virtualFile.isValid()) {
            return null
        }

        return SerializationHelper.provideSerializationPath(virtualFile, virtualFile.target)
    }

    override fun getCustomSerializedHeaderFile(serializationPath: String, project: Project, currentFile: VirtualFile): VirtualFile? =
        SerializationHelper.getCustomSerializedHeaderFile(serializationPath, project)

    private fun getHeader(import: String, configuration: XCBuildConfiguration): VirtualFile? {
        val adjustedName = adjustHeaderName(import)

        val project = configuration.project

        val parts: List<String> = adjustedName.split("/")
        if (parts.size != 2) return null

        val frameworkName = parts[0]
        val headerName = StringUtil.trimEnd(parts[1], ".h")
        if (frameworkName != headerName) return null

        val target = XcodeMetaData.getInstance(project).allProjects
            .asSequence()
            .flatMap { it.getTargets<PBXTarget>(null).asSequence() }
            .find { it.name == frameworkName }

        return target?.let { KonanBridgeFileManager.getInstance(project).forTarget(target, adjustedName) }
    }

    private object SerializationHelper : CustomTargetHeaderSerializationHelper("KONAN_BRIDGE") {
        override fun produceFile(target: PBXTarget, headerName: String, project: Project): VirtualFile =
            KonanBridgeFileManager.getInstance(project).forTarget(target, headerName)

        override fun checkTarget(target: PBXTarget, headerName: String): Boolean = target.konanHeader == headerName

        private val PBXTarget.konanHeader: String
            get() {
                ApplicationManager.getApplication().assertReadAccessAllowed()
                return "$name/$name.h"
            }
    }

    private fun Project.isKonanProject(): Boolean {
        return CachedValuesManager.getManager(this).getCachedValue(this) {
            val metaData = XcodeMetaData.getInstance(this)
            val value = readAction { metaData.workspaceFrameworks.any { framework -> framework.isKonanFramework() } }
            val tracker = metaData.xcodeProjectTrackers.buildSettingsTracker
            CachedValueProvider.Result(value, tracker)
        }
    }

    private fun LocalFramework.isKonanFramework(): Boolean {
        //todo[medvedev] implement correct check
        return name.contains("Kotlin")
//        return XcodeMetaData.asXCResolveConfiguration(configuration)?.buildConfiguration?.declaredBuildSettingValues?.getString("KOTLIN_NATIVE_PRESET") != null
    }
}

