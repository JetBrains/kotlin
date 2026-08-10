/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.backend.jvm.jvmTypeMapper
import org.jetbrains.kotlin.platform.has
import org.jetbrains.kotlin.platform.jvm.JvmPlatform

/**
 * Creates a [FirJvmTypeMapper] for the given [analysisSession].
 *
 * The type mapper is only registered in JVM sessions. For non-JVM sessions, a custom instance is created so that Java-like types can still
 * be generated for multiplatform source sets.
 */
internal fun createFirJvmTypeMapper(analysisSession: KaFirSession): FirJvmTypeMapper {
    val rootModuleSession = analysisSession.resolutionFacade.useSiteFirSession
    return when {
        analysisSession.targetPlatform.has<JvmPlatform>() -> rootModuleSession.jvmTypeMapper
        else -> FirJvmTypeMapper(rootModuleSession)
    }
}
