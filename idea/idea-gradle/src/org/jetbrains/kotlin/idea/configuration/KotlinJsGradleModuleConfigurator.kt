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

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.idea.versions.MAVEN_JS_STDLIB_ID
import org.jetbrains.kotlin.resolve.DefaultBuiltInPlatforms
import org.jetbrains.kotlin.resolve.TargetPlatform

class KotlinJsGradleModuleConfigurator : KotlinWithGradleConfigurator() {
    override val name: String = "gradle-js"
    override val presentableText: String = "JavaScript with Gradle"
    override val targetPlatform: TargetPlatform = DefaultBuiltInPlatforms.jsPlatform
    override val kotlinPluginName: String = KOTLIN_JS
    override fun getKotlinPluginExpression(forKotlinDsl: Boolean): String =
        if (forKotlinDsl) "id(\"kotlin2js\")" else "id 'kotlin2js'"
    override fun getMinimumSupportedVersion() = "1.1.0"
    override fun getStdlibArtifactName(sdk: Sdk?, version: String): String = MAVEN_JS_STDLIB_ID

    override fun addElementsToFile(file: PsiFile, isTopLevelProjectFile: Boolean, version: String): Boolean {
        val gradleVersion = fetchGradleVersion(file)

        if (getManipulator(file).useNewSyntax(kotlinPluginName, gradleVersion)) {
            val settingsPsiFile = if (isTopLevelProjectFile) {
                file.module?.getTopLevelBuildScriptSettingsPsiFile()
            } else {
                file.module?.getBuildScriptSettingsPsiFile()
            }
            if (settingsPsiFile != null) {
                getManipulator(settingsPsiFile).addResolutionStrategy("kotlin2js")
            }
        }

        return super.addElementsToFile(file, isTopLevelProjectFile, version)
    }

    companion object {
        val KOTLIN_JS = "kotlin2js"
    }
}
