/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.lifetime

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeTokenFactory

public abstract class KtLifetimeTokenProvider {
    public abstract fun getLifetimeTokenFactory(): KaLifetimeTokenFactory

    public companion object {
        public fun getService(project: Project): KtLifetimeTokenProvider =
            project.getService(KtLifetimeTokenProvider::class.java)
    }
}
