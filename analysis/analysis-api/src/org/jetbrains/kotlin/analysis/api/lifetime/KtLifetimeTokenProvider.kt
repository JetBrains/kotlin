/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.lifetime

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals

@KaAnalysisApiInternals
public abstract class KaLifetimeTokenProvider {
    public abstract fun getLifetimeTokenFactory(): KaLifetimeTokenFactory

    public companion object {
        @KaAnalysisApiInternals
        public fun getService(project: Project): KaLifetimeTokenProvider =
            project.getService(KaLifetimeTokenProvider::class.java)
    }
}

@KaAnalysisApiInternals
public typealias KtLifetimeTokenProvider = KaLifetimeTokenProvider

@KaAnalysisApiInternals
public class KaReadActionConfinementLifetimeTokenProvider : KaLifetimeTokenProvider() {
    override fun getLifetimeTokenFactory(): KaLifetimeTokenFactory {
        return KaReadActionConfinementLifetimeTokenFactory
    }
}

@KaAnalysisApiInternals
public typealias KtReadActionConfinementLifetimeTokenProvider = KaReadActionConfinementLifetimeTokenProvider