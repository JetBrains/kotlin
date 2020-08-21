/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.jetbrains.kmm.ios.PathManagerCustomization
import com.jetbrains.kmm.versions.KmmCompatibilityChecker

class KmmPluginStartupActivity : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        KmmCompatibilityChecker.checkCompatibilityAgainstBigKotlin(project)
        PathManagerCustomization.fixExecutionPermissionForBridgeService()
    }
}