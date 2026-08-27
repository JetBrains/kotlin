/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * An extension point for platforms hosting the Kotlin Analysis API to receive callbacks on analysis session creation, entry, and exit.
 *
 * In the absence of exceptions, the lifecycle of a listener is as follows:
 * 1. [beforeAcquiringSession] is called for each listener.
 * 2. The session is created.
 * 3. [afterAcquiringSession] is called for each listener.
 * 4. [beforeEnteringAnalysis] is called for each listener.
 * 5. The analysis body runs.
 * 6. [afterLeavingAnalysis] is called for each listener.
 *
 * If an exception is thrown by a listener implementation method itself, it is caught, logged via [Logger.error],
 * and isolated so that it does not crash analysis or prevent remaining listeners from executing.
 */
@KaPlatformInterface
public interface KaSessionListener {
    /**
     * Called on entry to the [org.jetbrains.kotlin.analysis.api.session.analyze] call,
     * before the [org.jetbrains.kotlin.analysis.api.KaSession] is looked up for the use-site module or element.
     */
    public fun beforeAcquiringSession(useSiteModule: KaModule, useSiteElement: PsiElement?) {}

    /**
     * Called when an exception is thrown during [org.jetbrains.kotlin.analysis.api.KaSession] creation.
     */
    public fun onSessionAcquisitionException(useSiteModule: KaModule, useSiteElement: PsiElement?, throwable: Throwable) {}

    /**
     * Called after a [org.jetbrains.kotlin.analysis.api.KaSession] has been created, or after [onSessionAcquisitionException] if an exception occurred.
     */
    public fun afterAcquiringSession(useSiteModule: KaModule, useSiteElement: PsiElement?) {}

    /**
     * Called immediately after [afterAcquiringSession] and before the body of the analysis block is executed.
     */
    public fun beforeEnteringAnalysis(useSiteModule: KaModule, useSiteElement: PsiElement?) {}

    /**
     * Called when an exception is thrown by the analysis block.
     */
    public fun onAnalysisException(useSiteModule: KaModule, useSiteElement: PsiElement?, throwable: Throwable) {}

    /**
     * Called after the analysis block is finished, or after [onAnalysisException] if an exception occurred.
     */
    public fun afterLeavingAnalysis(useSiteModule: KaModule, useSiteElement: PsiElement?) {}

    @KaPlatformInterface
    public companion object {
        public val EP_NAME: ExtensionPointName<KaSessionListener> =
            ExtensionPointName("org.jetbrains.kotlin.kaSessionListener")
    }
}
