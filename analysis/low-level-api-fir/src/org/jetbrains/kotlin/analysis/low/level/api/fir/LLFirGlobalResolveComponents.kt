/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModule

internal class LLFirGlobalResolveComponents(
    val project: Project,
) {
    val phaseRunner: LLFirPhaseRunner = LLFirPhaseRunner()
    val lockProvider: LLFirLockProvider = LLFirLockProvider()
}