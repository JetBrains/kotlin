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

package org.jetbrains.kotlin.android

import com.android.tools.idea.npw.template.ConvertJavaToKotlinProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.android.configure.KotlinAndroidGradleModuleConfigurator
import org.jetbrains.kotlin.idea.actions.JavaToKotlinAction
import org.jetbrains.kotlin.idea.configuration.ConfigureKotlinStatus
import org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator
import org.jetbrains.kotlin.idea.configuration.excludeSourceRootModules
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.idea.versions.bundledRuntimeVersion

class ConvertJavaToKotlinProviderImpl : ConvertJavaToKotlinProvider {
    override fun configureKotlin(project: Project) {
        val configurator = KotlinProjectConfigurator.EP_NAME.findExtension(KotlinAndroidGradleModuleConfigurator::class.java)
        val nonConfiguredModules = project.allModules().excludeSourceRootModules().filter {
            configurator.getStatus(it) == ConfigureKotlinStatus.CAN_BE_CONFIGURED
        }
        configurator.configureSilently(project, nonConfiguredModules, bundledRuntimeVersion())
    }

    override fun getKotlinVersion(): String {
        return bundledRuntimeVersion()
    }

    override fun convertToKotlin(project: Project, files: List<PsiJavaFile>): List<PsiFile> {
        return JavaToKotlinAction.convertFiles(files, project, askExternalCodeProcessing = false)
    }
}
