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

import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.psi.PsiElement

interface GradleBuildScriptManipulator {
    fun isConfigured(kotlinPluginName: String): Boolean

    fun configureModuleBuildScript(kotlinPluginName: String,
                                   stdlibArtifactName: String,
                                   version: String,
                                   jvmTarget: String?): Boolean

    fun configureProjectBuildScript(version: String): Boolean

    fun changeCoroutineConfiguration(coroutineOption: String): PsiElement?

    fun changeLanguageVersion(version: String, forTests: Boolean): PsiElement?

    fun changeApiVersion(version: String, forTests: Boolean): PsiElement?

    fun addKotlinLibraryToModuleBuildScript(
            scope: DependencyScope,
            libraryDescriptor: ExternalLibraryDescriptor,
            isAndroidModule: Boolean)

    fun getKotlinStdlibVersion(): String?
}