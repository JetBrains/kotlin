/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.framework

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.configuration.KotlinJsModuleConfigurator

/**
 * @param project null when project doesn't exist yet (called from project wizard)
 */
class JSLibraryStdDescription(project: Project?) : CustomLibraryDescriptorWithDeferredConfig(
    project,
    KotlinJsModuleConfigurator.NAME,
    LIBRARY_NAME,
    DIALOG_TITLE,
    LIBRARY_CAPTION,
    JSLibraryKind,
    SUITABLE_LIBRARY_KINDS
) {

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
