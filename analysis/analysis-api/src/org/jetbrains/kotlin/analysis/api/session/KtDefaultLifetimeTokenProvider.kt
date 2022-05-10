/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.session

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.tokens.KtLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.tokens.ReadActionConfinementValidityTokenFactory

public abstract class KtDefaultLifetimeTokenProvider {
    public abstract fun getDefaultLifetimeTokenFactory(): KtLifetimeTokenFactory

    public companion object {
        public fun getService(project: Project): KtDefaultLifetimeTokenProvider =
            project.getService(KtDefaultLifetimeTokenProvider::class.java)
    }
}

public class KtReadActionConfinementDefaultLifetimeTokenProvider: KtDefaultLifetimeTokenProvider() {
    override fun getDefaultLifetimeTokenFactory(): KtLifetimeTokenFactory {
        return ReadActionConfinementValidityTokenFactory
    }
}