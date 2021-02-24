/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.codeInsight.gradle.MultiplePluginVersionGradleImportingTestCase
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.plugins.gradle.execution.test.runner.GradleTestRunConfigurationProducer

fun MultiplePluginVersionGradleImportingTestCase.findTasksToRun(file: VirtualFile): List<String>? {
    return runReadAction {
        GradleTestRunConfigurationProducer.findAllTestsTaskToRun(file, project)
            .flatMap { it.tasks }
            .sorted()
    }
}