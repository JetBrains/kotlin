/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.framework

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.libraries.LibraryKind
import org.jetbrains.kotlin.idea.KotlinJvmBundle
import org.jetbrains.kotlin.idea.compiler.configuration.Kotlin2JvmCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.configuration.KotlinJavaModuleConfigurator
import org.jetbrains.kotlin.idea.versions.bundledRuntimeVersion
import org.jetbrains.kotlin.idea.versions.getDefaultJvmTarget

/**
 * @param project null when project doesn't exist yet (called from project wizard)
 */
class JavaRuntimeLibraryDescription(project: Project?) : CustomLibraryDescriptorWithDeferredConfig(
    project,
    KotlinJavaModuleConfigurator.NAME,
    LIBRARY_NAME,
    DIALOG_TITLE,
    LIBRARY_CAPTION,
    KOTLIN_JAVA_RUNTIME_KIND,
    SUITABLE_LIBRARY_KINDS
) {

    override fun configureKotlinSettings(project: Project, sdk: Sdk?) {
        val defaultJvmTarget = getDefaultJvmTarget(sdk, bundledRuntimeVersion())
        if (defaultJvmTarget != null) {
            Kotlin2JvmCompilerArgumentsHolder.getInstance(project).update {
                jvmTarget = defaultJvmTarget.description
            }
        }
    }

    companion object {
        val KOTLIN_JAVA_RUNTIME_KIND: LibraryKind = LibraryKind.create("kotlin-java-runtime")
        const val LIBRARY_NAME = "KotlinJavaRuntime"

        val JAVA_RUNTIME_LIBRARY_CREATION get() = KotlinJvmBundle.message("java.runtime.library.creation")
        val DIALOG_TITLE get() = KotlinJvmBundle.message("create.kotlin.java.runtime.library")
        val LIBRARY_CAPTION get() = KotlinJvmBundle.message("kotlin.java.runtime.library")
        val SUITABLE_LIBRARY_KINDS: Set<LibraryKind> = setOf(KOTLIN_JAVA_RUNTIME_KIND)
    }
}
