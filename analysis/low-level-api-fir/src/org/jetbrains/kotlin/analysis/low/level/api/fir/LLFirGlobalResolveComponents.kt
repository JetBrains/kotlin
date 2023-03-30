/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirLazyResolveContractChecker

internal class LLFirGlobalResolveComponents(val project: Project) {
    companion object {
        fun getInstance(project: Project): LLFirGlobalResolveComponents {
            return project.getService(LLFirGlobalResolveComponents::class.java)
        }
    }

    val checker: LLFirLazyResolveContractChecker = LLFirLazyResolveContractChecker()
    val lockProvider: LLFirLockProvider = LLFirLockProvider(checker)
}