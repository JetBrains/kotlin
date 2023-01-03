/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile

public abstract class KotlinBuiltInsCache {
    public abstract fun getOrLoadBuiltIns(project: Project): Collection<KtDecompiledFile>

    public companion object {
        public fun getInstance(): KotlinBuiltInsCache? =
            ApplicationManager.getApplication().getService(KotlinBuiltInsCache::class.java)
    }
}