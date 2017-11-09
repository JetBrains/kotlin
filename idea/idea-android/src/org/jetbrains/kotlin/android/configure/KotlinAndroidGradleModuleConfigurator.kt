/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.android.configure

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.configuration.KotlinWithGradleConfigurator
import org.jetbrains.kotlin.idea.util.projectStructure.version
import org.jetbrains.kotlin.idea.versions.MAVEN_STDLIB_ID_JDK7
import org.jetbrains.kotlin.idea.versions.MAVEN_STDLIB_ID_JRE7
import org.jetbrains.kotlin.idea.versions.hasJreSpecificRuntime
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

class KotlinAndroidGradleModuleConfigurator internal constructor() : KotlinWithGradleConfigurator() {

    override val name: String = NAME

    override val targetPlatform: TargetPlatform = JvmPlatform

    override val presentableText: String = "Android with Gradle"

    public override fun isApplicable(module: Module): Boolean = KotlinPluginUtil.isAndroidGradleModule(module)

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
