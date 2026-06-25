/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.psi.KtElement

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
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaSessionListener {
    /**
     * Called on entry to the [org.jetbrains.kotlin.analysis.api.session.analyze] call,
     * before the [KaSession] is looked up for the use-site module or element.
     */
    public fun beforeAcquiringSession(useSiteModule: KaModule, useSiteElement: KtElement?) {}

    /**
     * Called when an exception is thrown during [KaSession] creation.
     */
    public fun onSessionAcquisitionException(useSiteModule: KaModule, useSiteElement: KtElement?, throwable: Throwable) {}

    /**
     * Called after a [KaSession] has been created, or after [onSessionAcquisitionException] if an exception occurred.
     */
    public fun afterAcquiringSession(useSiteModule: KaModule, useSiteElement: KtElement?) {}

    /**
     * Called immediately after [afterAcquiringSession] and before the body of the analysis block is executed.
     */
    public fun beforeEnteringAnalysis(session: KaSession, useSiteElement: KtElement?) {}

    /**
     * Called when an exception is thrown by the analysis block.
     */
    public fun onAnalysisException(session: KaSession, useSiteElement: KtElement?, throwable: Throwable) {}

    /**
     * Called after the analysis block is finished, or after [onAnalysisException] if an exception occurred.
     */
    public fun afterLeavingAnalysis(session: KaSession, useSiteElement: KtElement?) {}

    public companion object {
        public val EP_NAME: ExtensionPointName<KaSessionListener> =
            ExtensionPointName("org.jetbrains.kotlin.kaSessionListener")
    }
}
