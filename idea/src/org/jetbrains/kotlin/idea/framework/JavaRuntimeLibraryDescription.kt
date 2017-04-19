/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.framework

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.libraries.LibraryKind
import org.jetbrains.kotlin.idea.compiler.configuration.Kotlin2JvmCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.configuration.KotlinJavaModuleConfigurator
import org.jetbrains.kotlin.idea.versions.bundledRuntimeVersion
import org.jetbrains.kotlin.idea.versions.getDefaultJvmTarget

/**
 * @param project null when project doesn't exist yet (called from project wizard)
 */
class JavaRuntimeLibraryDescription(project: Project?) :
        CustomLibraryDescriptorWithDeferredConfig(project,
                                                  KotlinJavaModuleConfigurator.NAME,
                                                  LIBRARY_NAME,
                                                  DIALOG_TITLE,
                                                  LIBRARY_CAPTION,
                                                  KOTLIN_JAVA_RUNTIME_KIND,
                                                  SUITABLE_LIBRARY_KINDS) {

    override fun configureKotlinSettings(project: Project, sdk: Sdk?) {
        val defaultJvmTarget = getDefaultJvmTarget(sdk, bundledRuntimeVersion())
        if (defaultJvmTarget != null) {
            Kotlin2JvmCompilerArgumentsHolder.getInstance(project).update {
                jvmTarget = defaultJvmTarget.description
            }
        }
    }

    companion object {
        val KOTLIN_JAVA_RUNTIME_KIND = LibraryKind.create("kotlin-java-runtime")
        val LIBRARY_NAME = "KotlinJavaRuntime"

        val JAVA_RUNTIME_LIBRARY_CREATION = "Java Runtime Library Creation"
        val DIALOG_TITLE = "Create Kotlin Java Runtime Library"
        val LIBRARY_CAPTION = "Kotlin Java Runtime Library"
        val SUITABLE_LIBRARY_KINDS: Set<LibraryKind> = setOf(KOTLIN_JAVA_RUNTIME_KIND)
    }
}
