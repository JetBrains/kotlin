/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf.util

import org.jetbrains.kotlin.idea.testFramework.ProjectOpenAction

/**
 * @author Vladimir Ilmov
 */
class ExternalProject(val path: String, val openWith: ProjectOpenAction) {
    companion object {
        const val KOTLIN_PROJECT_PATH = "../perfTestProject"

        val KOTLIN_GRADLE = ExternalProject(KOTLIN_PROJECT_PATH, ProjectOpenAction.GRADLE_PROJECT)
        val KOTLIN_JPS = ExternalProject(KOTLIN_PROJECT_PATH, ProjectOpenAction.EXISTING_IDEA_PROJECT)
        val KOTLIN_AUTO = ExternalProject(KOTLIN_PROJECT_PATH, autoOpenAction(KOTLIN_PROJECT_PATH))

        fun autoOpenAction(path: String): ProjectOpenAction {
            return if (exists(path, ".idea", "modules.xml"))
                ProjectOpenAction.EXISTING_IDEA_PROJECT.apply { println("Opening $path in iml mode.") }
            else
                ProjectOpenAction.GRADLE_PROJECT.apply { println("Opening $path in Gradle mode.") }
        }
    }
}
