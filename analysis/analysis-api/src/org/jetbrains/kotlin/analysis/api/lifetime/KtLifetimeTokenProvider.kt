/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.lifetime

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals

@KtAnalysisApiInternals
public abstract class KtLifetimeTokenProvider {
    public abstract fun getLifetimeTokenFactory(): KtLifetimeTokenFactory

    public companion object {
        @KtAnalysisApiInternals
        public fun getService(project: Project): KtLifetimeTokenProvider =
            project.getService(KtLifetimeTokenProvider::class.java)
    }
}

@KtAnalysisApiInternals
public class KtReadActionConfinementLifetimeTokenProvider : KtLifetimeTokenProvider() {
    override fun getLifetimeTokenFactory(): KtLifetimeTokenFactory {
        return KtReadActionConfinementLifetimeTokenFactory
    }
}