package org.jetbrains.kotlin.analysis.api.session

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.psi.KtElement

/**
 * A runner for [KaSessionListener] hooks that properly manages the listener lifecycle, including
 * ordered invocation and exception handling.
 *
 * It ensures that any listener which successfully completes its `before` block will also execute its corresponding
 * `after` block, even in the presence of exceptions.
 */
@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
public abstract class KaSessionListenerRunner(public val project: Project) {
    protected abstract fun KaSessionListener.before()
    protected abstract fun KaSessionListener.onException(throwable: Throwable)
    protected abstract fun KaSessionListener.after()

    private var successfulListenersCount: Int = 0
    private val listeners: List<KaSessionListener> = KaSessionListener.EP_NAME.getExtensionList(project)

    /**
     * Executes the `before` block for all registered [KaSessionListener]s in forward extension point order.
     * Keeps track of the number of listeners that successfully complete this phase.
     */
    @KaImplementationDetail
    public fun runListenersBeforeBlock() {
        for (i in listeners.indices) {
            listeners[i].before()
            successfulListenersCount++
        }
    }

    /**
     * Handles an exception thrown during the listened event itself or by a subsequent `before` block.
     * Iterates over all successfully started listeners in reverse order, notifying them of the exception.
     *
     * @return The potentially wrapped or modified exception to be rethrown.
     */
    @KaImplementationDetail
    public fun handleException(throwable: Throwable): Throwable = iterateListenersOnException(successfulListenersCount, throwable)

    /**
     * Executes the `after` block for all listeners that successfully completed their `before` block.
     * Listeners are invoked in reverse extension point order. If an exception occurs during an `after` block,
     * it is propagated back to the preceding listeners via [onException].
     */
    @KaImplementationDetail
    public fun runListenersAfterBlock() {
        var currentException: Throwable? = null
        for (listenerIndex in successfulListenersCount - 1 downTo 0) {
            try {
                listeners[listenerIndex].after()
            } catch (e: Throwable) {
                currentException = iterateListenersOnException(listenerIndex, e)
            }
        }
        currentException?.let { throw it }
    }

    private fun iterateListenersOnException(
        indexOfListenerWithException: Int,
        exception: Throwable,
    ): Throwable {
        var currentException = exception
        for (i in indexOfListenerWithException - 1 downTo 0) {
            try {
                listeners[i].onException(currentException)
            } catch (e: Throwable) {
                currentException = e
            }
        }
        return currentException
    }
}

@KaImplementationDetail
public class KaSessionAcquisitionListenerRunner(
    project: Project,
    public val useSiteModule: KaModule,
    public val useSiteElement: KtElement?,
) : KaSessionListenerRunner(project) {
    override fun KaSessionListener.before() {
        beforeAcquiringSession(useSiteModule, useSiteElement)
    }

    override fun KaSessionListener.onException(throwable: Throwable) {
        onSessionAcquisitionException(useSiteModule, useSiteElement, throwable)
    }

    override fun KaSessionListener.after() {
        afterAcquiringSession(useSiteModule, useSiteElement)
    }
}

@KaImplementationDetail
public class KaAnalysisListenerRunner(
    project: Project,
    public val session: KaSession,
    public val useSiteElement: KtElement?,
) : KaSessionListenerRunner(project) {
    override fun KaSessionListener.before() {
        beforeEnteringAnalysis(session, useSiteElement)
    }

    override fun KaSessionListener.onException(throwable: Throwable) {
        onAnalysisException(session, useSiteElement, throwable)
    }

    override fun KaSessionListener.after() {
        afterLeavingAnalysis(session, useSiteElement)
    }
}
