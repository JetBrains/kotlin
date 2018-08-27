/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle

import com.intellij.openapi.project.Project
import org.jetbrains.konan.settings.KonanProjectComponent
import org.jetbrains.plugins.gradle.settings.GradleSettings

class GradleKonanProjectComponent(project: Project) : KonanProjectComponent(project) {
    override fun looksLikeKotlinNativeProject(): Boolean {
        //TODO not just any gradle project
        return GradleSettings.getInstance(project).linkedProjectsSettings.isNotEmpty()
    }
}