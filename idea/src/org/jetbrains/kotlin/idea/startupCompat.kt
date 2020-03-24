/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.util.runWhenSmart

// BUNCH: 201

fun runActivity(project: Project) {
    project.runWhenSmart {
        val daemonCodeAnalyzer = DaemonCodeAnalyzerImpl.getInstanceEx(project) as DaemonCodeAnalyzerImpl
        daemonCodeAnalyzer.runLocalInspectionPassAfterCompletionOfGeneralHighlightPass(true)
    }
}