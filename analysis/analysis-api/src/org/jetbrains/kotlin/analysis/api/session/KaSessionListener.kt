package org.jetbrains.kotlin.analysis.api.session

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.psi.KtElement

/**
 * An extension point for platforms hosting the Kotlin Analysis API to receive callbacks on analysis session creation, entry, and exit.
 *
 * In the absence of exceptions, the lifecycle of a listener is as follows:
 * 1. [beforeAcquiringSession] is called in EP forward order.
 * 2. The session is created.
 * 3. [afterAcquiringSession] is called in EP reverse order.
 * 4. [beforeEnteringAnalysis] is called in EP forward order.
 * 5. The analysis body runs.
 * 6. [afterLeavingAnalysis] is called in EP reverse order.
 *
 * Listeners receive a consistent view of the session and any failures that happen within it.
 * The exception handling ensures that any listener that receives a before callback for an event *must* receive the after callback for that event.
 * Effectively, each listener invocation is wrapped in a structure similar to:
 * ```kotlin
 * fun process() {
 *     before()
 *     try {
 *         if (nextListener != null) {
 *             nextListener.process()
 *         } else {
 *             acquireAnalysisSession() / runAnalysis()
 *         }
 *     } catch (e: Throwable) {
 *         onException(e)
 *     } finally {
 *         after()
 *     }
 * }
 * ```
 */
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaSessionListener {
    /**
     * Called on entry to the [analyze] call, before the [KaSession] is looked up for the use-site module or element.
     * Called in forward EP order.
     */
    public fun beforeAcquiringSession(useSiteModule: KaModule, useSiteElement: KtElement?) {}

    /**
     * Called when an exception is thrown during [KaSession] creation or by a subsequent listener's [beforeAcquiringSession].
     * Called in reverse EP order.
     *
     * Any exceptions thrown by this method will replace the original exception for any further callbacks.
     */
    public fun onSessionAcquisitionException(useSiteModule: KaModule, useSiteElement: KtElement?, throwable: Throwable) {}

    /**
     * Called after a [KaSession] has been created, or after [onSessionAcquisitionException] if an exception occurred.
     * Called in reverse EP order.
     */
    public fun afterAcquiringSession(useSiteModule: KaModule, useSiteElement: KtElement?) {}

    /**
     * Called immediately after [afterAcquiringSession] and before the body of the analysis block is executed.
     * Called in forward EP order.
     */
    public fun beforeEnteringAnalysis(session: KaSession, useSiteElement: KtElement?) {}

    /**
     * Called when an exception is thrown by the analysis block or by a subsequent listener's [beforeEnteringAnalysis].
     * Called in reverse EP order.
     *
     * Any exceptions thrown by this method will replace the original exception for any further callbacks.
     */
    public fun onAnalysisException(session: KaSession, useSiteElement: KtElement?, throwable: Throwable) {}

    /**
     * Called after the analysis block is finished, or after [onAnalysisException] if an exception occurred.
     * Called in reverse EP order.
     */
    public fun afterLeavingAnalysis(session: KaSession, useSiteElement: KtElement?) {}

    public companion object {
        public val EP_NAME: ExtensionPointName<KaSessionListener> =
            ExtensionPointName("org.jetbrains.kotlin.kaSessionListener")
    }
}
