/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

/**
 * Provides the FIR extensions required to analyze a script. Without the service, scripts are analyzed with no script-specific extensions.
 */
@LLFirInternals
interface LLFirScriptExtensionRegistrarProvider {
    fun getExtensionRegistrars(module: KaScriptModule): List<FirExtensionRegistrar>

    @LLFirInternals
    companion object {
        fun getInstance(project: Project): LLFirScriptExtensionRegistrarProvider? = project.serviceOrNull()
    }
}
