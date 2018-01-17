/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.configure

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.configuration.AndroidGradle
import org.jetbrains.kotlin.idea.configuration.KotlinWithGradleConfigurator
import org.jetbrains.kotlin.idea.configuration.getBuildSystemType
import org.jetbrains.kotlin.idea.util.projectStructure.version
import org.jetbrains.kotlin.idea.versions.MAVEN_STDLIB_ID_JDK7
import org.jetbrains.kotlin.idea.versions.hasJreSpecificRuntime
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

class KotlinAndroidGradleModuleConfigurator internal constructor() : KotlinWithGradleConfigurator() {

    override val name: String = NAME

    override val targetPlatform: TargetPlatform = JvmPlatform

    override val presentableText: String = "Android with Gradle"

    public override fun isApplicable(module: Module): Boolean = module.getBuildSystemType() == AndroidGradle

    override val kotlinPluginName: String = KOTLIN_ANDROID

    override fun addElementsToFile(file: PsiFile, isTopLevelProjectFile: Boolean, version: String): Boolean {
        val manipulator = getManipulator(file)
        val sdk = ModuleUtil.findModuleForPsiElement(file)?.let { ModuleRootManager.getInstance(it).sdk }
        val jvmTarget = getJvmTarget(sdk, version)

        return if (isTopLevelProjectFile) {
            manipulator.configureProjectBuildScript(version)
        }
        else {
            manipulator.configureModuleBuildScript(
                    kotlinPluginName,
                    getStdlibArtifactName(sdk, version),
                    version,
                    jvmTarget
            )
        }
    }

    override fun getStdlibArtifactName(sdk: Sdk?, version: String): String {
        if (sdk != null && hasJreSpecificRuntime(version)) {
            val sdkVersion = sdk.version
            if (sdkVersion != null && sdkVersion.isAtLeast(JavaSdkVersion.JDK_1_8)) {
                // Android dex can't convert our kotlin-stdlib-jre8 artifact, so use jre7 instead (KT-16530)
                return MAVEN_STDLIB_ID_JDK7
            }
        }

        return super.getStdlibArtifactName(sdk, version)
    }

    companion object {
        private val NAME = "android-gradle"

        private val KOTLIN_ANDROID = "kotlin-android"
    }
}
