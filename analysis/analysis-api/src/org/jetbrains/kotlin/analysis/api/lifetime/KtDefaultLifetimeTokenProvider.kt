/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.lifetime

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals

@Suppress("DEPRECATION")
@KtAnalysisApiInternals
@Service(Service.Level.PROJECT)
@Deprecated("Needed for binary compatibility, see KTIJ-27188")
public class KtDefaultLifetimeTokenProvider(private val project: Project) {
    public fun getDefaultLifetimeTokenFactory(): KtLifetimeTokenFactory {
        return KtLifetimeTokenProvider.getService(project).getLifetimeTokenFactory()
    }

    public companion object {
        @KtAnalysisApiInternals
        public fun getService(project: Project): KtDefaultLifetimeTokenProvider =
            project.getService(KtDefaultLifetimeTokenProvider::class.java)
    }
}
