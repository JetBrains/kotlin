/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import com.intellij.build.BuildContentManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.KotlinIdeaGradleBundle
import org.jetbrains.kotlin.idea.util.application.invokeLater
import org.jetbrains.kotlin.idea.util.application.isUnitTestMode

internal fun handleError(project: Project, errors: List<KotlinDslScriptModel.Message>) {
    if (isUnitTestMode()) {
        throw IllegalStateException(
            KotlinIdeaGradleBundle.message("title.kotlin.build.script")
                    + ":\n"
                    + errors.joinToString("\n") { it.text + "\n" + it.details }
        )
    }
    invokeLater(project.disposed) {
        BuildContentManager.getInstance(project).orCreateToolWindow.activate(null, false, false)
    }
}
