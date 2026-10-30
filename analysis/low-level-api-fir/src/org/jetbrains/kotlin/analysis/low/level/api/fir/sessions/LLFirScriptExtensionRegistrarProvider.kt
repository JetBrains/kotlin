/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

/**
 * Provides the FIR extensions required to analyze a script. Without providers, scripts are analyzed with no script-specific extensions.
 */
@LLFirInternals
interface LLFirScriptExtensionRegistrarProvider {
    @LLFirInternals
    companion object : ProjectExtensionDescriptor<LLFirScriptExtensionRegistrarProvider>(
        "org.jetbrains.kotlin.llFirScriptExtensionRegistrarProvider",
        LLFirScriptExtensionRegistrarProvider::class.java
    )

    fun getExtensionRegistrars(module: KaScriptModule): List<FirExtensionRegistrar>
}
