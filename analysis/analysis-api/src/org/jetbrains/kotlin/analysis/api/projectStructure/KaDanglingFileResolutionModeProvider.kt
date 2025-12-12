/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.projectStructure

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.serviceOrNull
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.psi.KtFile

/**
 * A service that provides [KaDanglingFileResolutionMode] for a given [KtFile] based purely on its content.
 */
@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaDanglingFileResolutionModeProvider {
    /**
     * Calculates [KaDanglingFileResolutionMode] for the given [file].
     *
     * This mode is calculated based solely on the difference between
     * the given dangling [file] and its [copyOrigin][KtFile.copyOrigin].
     * If there was an out-of-block modification made in the [file] compared to its [copyOrigin][KtFile.copyOrigin],
     * returns [KaDanglingFileResolutionMode.PREFER_SELF]. Otherwise, returns [KaDanglingFileResolutionMode.IGNORE_SELF].
     *
     * If the given [file] is not a dangling one, returns [KaDanglingFileResolutionMode.PREFER_SELF].
     */
    @KaImplementationDetail
    public fun calculateMode(file: KtFile): KaDanglingFileResolutionMode

    @KaImplementationDetail
    public companion object {
        /**
         * Calculates [KaDanglingFileResolutionMode] for the given [file].
         *
         * This mode is calculated based solely on the difference between
         * the given dangling [file] and its [copyOrigin][KtFile.copyOrigin].
         * If there was an out-of-block modification made in the [file] compared to its [copyOrigin][KtFile.copyOrigin],
         * returns [KaDanglingFileResolutionMode.PREFER_SELF]. Otherwise, returns [KaDanglingFileResolutionMode.IGNORE_SELF].
         *
         * If the given [file] is not a dangling one, returns [KaDanglingFileResolutionMode.PREFER_SELF].
         */
        @KaImplementationDetail
        public fun calculateMode(file: KtFile): KaDanglingFileResolutionMode =
            ApplicationManager.getApplication().serviceOrNull<KaDanglingFileResolutionModeProvider>()?.calculateMode(file)
                ?: KaDanglingFileResolutionMode.PREFER_SELF
    }
}