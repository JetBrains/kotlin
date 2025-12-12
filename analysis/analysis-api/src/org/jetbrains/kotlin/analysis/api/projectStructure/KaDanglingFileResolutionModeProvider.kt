/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.projectStructure

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.psi.KtFile

@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaDanglingFileResolutionModeProvider {
    public fun calculateMode(file: KtFile): KaDanglingFileResolutionMode

    public companion object {
        public fun getInstance(project: Project): KaDanglingFileResolutionModeProvider? = project.serviceOrNull()

        public fun calculateMode(file: KtFile, project: Project): KaDanglingFileResolutionMode =
            getInstance(project)?.calculateMode(file) ?: KaDanglingFileResolutionMode.PREFER_SELF
    }
}