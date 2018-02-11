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
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.configuration.KotlinJsModuleConfigurator

/**
 * @param project null when project doesn't exist yet (called from project wizard)
 */
class JSLibraryStdDescription(project: Project?) :
        CustomLibraryDescriptorWithDeferredConfig(
                project,
                KotlinJsModuleConfigurator.NAME,
                LIBRARY_NAME,
                DIALOG_TITLE,
                LIBRARY_CAPTION,
                JSLibraryKind,
                SUITABLE_LIBRARY_KINDS) {

    @TestOnly
    fun createNewLibraryForTests(): NewLibraryConfiguration {
        return createConfigurationFromPluginPaths()
    }

    companion object {
        val LIBRARY_NAME = "KotlinJavaScript"

        val JAVA_SCRIPT_LIBRARY_CREATION = "JavaScript Library Creation"
        val DIALOG_TITLE = "Create Kotlin JavaScript Library"
        val LIBRARY_CAPTION = "Kotlin JavaScript Library"
        val SUITABLE_LIBRARY_KINDS = setOf(JSLibraryKind)
    }
}
