/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.core.isAndroidModule
import org.jetbrains.kotlin.idea.versions.getDefaultJvmTarget
import org.jetbrains.kotlin.resolve.DefaultBuiltInPlatforms
import org.jetbrains.kotlin.resolve.TargetPlatform

class KotlinGradleModuleConfigurator : KotlinWithGradleConfigurator() {

    override val name: String
        get() = NAME

    override val targetPlatform: TargetPlatform
        get() = DefaultBuiltInPlatforms.jvmPlatform

    override val presentableText: String
        get() = "Java with Gradle"

    override val kotlinPluginName: String
        get() = KOTLIN

    override fun getKotlinPluginExpression(forKotlinDsl: Boolean): String =
        if (forKotlinDsl) "kotlin(\"jvm\")" else "id 'org.jetbrains.kotlin.jvm'"

    override fun getJvmTarget(sdk: Sdk?, version: String) = getDefaultJvmTarget(sdk, version)?.description

    override fun isApplicable(module: Module): Boolean {
        return super.isApplicable(module) && !module.isAndroidModule()
    }

    override fun configureModule(
        module: Module,
        file: PsiFile,
        isTopLevelProjectFile: Boolean,
        version: String, collector: NotificationMessageCollector,
        filesToOpen: MutableCollection<PsiFile>
    ) {
        super.configureModule(module, file, isTopLevelProjectFile, version, collector, filesToOpen)

        val moduleGroup = module.getWholeModuleGroup()
        for (sourceModule in moduleGroup.allModules()) {
            if (addStdlibToJavaModuleInfo(sourceModule, collector)) {
                break
            }
        }
    }

    companion object {
        val NAME = "gradle"
        val KOTLIN = "kotlin"
    }
}
