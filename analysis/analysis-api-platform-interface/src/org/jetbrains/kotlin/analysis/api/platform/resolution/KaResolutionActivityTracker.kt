/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.resolution

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.platform.KaEngineService

/**
 * This service provides a bridge between the Analysis API, Kotlin IntelliJ plugin and Java IntelliJ plugin.
 *
 * In particular, this service helps to mark periods when the thread is busy with Kotlin resolution. During this time
 * more strict rules may be applied on Java resolution, so it should take into account this to avoid Kotlin compiler contract violations.
 *
 * This service should be dropped as soon as it will be possible to implement the new suppression logic for
 * [com.intellij.psi.impl.PsiFileEx.BATCH_REFERENCE_PROCESSING] directly in the Kotlin repository (KT-73649 as a reference).
 */
@KaIdeApi
public interface KaResolutionActivityTracker : KaEngineService {
    /**
     * Whether the thread is currently executing Kotlin resolution logic.
     */
    public val isKotlinResolutionActive: Boolean

    @KaIdeApi
    public companion object {
        public fun getInstance(): KaResolutionActivityTracker? {
            return ApplicationManager.getApplication().serviceOrNull<KaResolutionActivityTracker>()
        }
    }
}
