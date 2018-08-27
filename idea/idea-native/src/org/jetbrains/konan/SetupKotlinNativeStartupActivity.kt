/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.jetbrains.konan.settings.KonanProjectComponent

class SetupKotlinNativeStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        //todo: looks like without default project component (like disabled Gradle plugin) we will get exception here (that's bad)
        if (!KonanProjectComponent.getInstance(project).looksLikeKotlinNativeProject()) return
        ensureKotlinNativeExists(project)
    }

    private fun ensureKotlinNativeExists(project: Project) {
        ApplicationManager.getApplication().assertIsDispatchThread()
        KotlinNativeToolchain.BUNDLED.ensureExists(project)
    }
}
