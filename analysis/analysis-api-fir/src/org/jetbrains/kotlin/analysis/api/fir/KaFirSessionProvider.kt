/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.LowMemoryWatcher
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.lifetime.KaFirLifetimeTracker
import org.jetbrains.kotlin.analysis.api.fir.permissions.KaFirWriteActionStartedChecker
import org.jetbrains.kotlin.analysis.api.fir.restrictedAnalysis.KaFirRestrictedAnalysisException
import org.jetbrains.kotlin.analysis.api.fir.sessions.KaFirUseSiteLibraryModuleAnalysisException
import org.jetbrains.kotlin.analysis.api.fir.utils.KaFirCacheCleaner
import org.jetbrains.kotlin.analysis.api.impl.base.util.withKaModuleEntry
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.permissions.KaAnalysisPermissionRegistry
import org.jetbrains.kotlin.analysis.api.platform.KaCachedService
import org.jetbrains.kotlin.analysis.api.platform.KaSessionListener
import org.jetbrains.kotlin.analysis.api.platform.KotlinAnalysisInWriteActionListener
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinReadActionConfinementLifetimeToken
import org.jetbrains.kotlin.analysis.api.platform.permissions.KaAnalysisPermissionChecker
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.platform.restrictedAnalysis.KotlinRestrictedAnalysisService
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.isResolvable
import org.jetbrains.kotlin.analysis.api.projectStructure.isStable
import org.jetbrains.kotlin.analysis.api.session.KaSessionProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.LLFirDeclarationModificationService
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.cache.LLFirSessionCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.cache.LLFirSessionInvalidationListener
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.cache.LLFirSessionInvalidationTopics
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.structure.LLSessionStructureWriter
import org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.LLStatisticsService
import org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.domains.LLAnalysisSessionStatistics
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.rethrowIntellijPlatformExceptionIfNeeded
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/**
 * [KaFirSessionProvider] keeps [KaFirSession]s in a cache, which are actively invalidated with their associated underlying
 * [LLFirSession][LLFirSession]s.
 */
internal class KaFirSessionProvider(project: Project) : KaSessionProvider(project), Disposable {
    // We cache several services to avoid repeated `getService` calls in `analyze`.
    @KaCachedService
    private val permissionChecker by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KaAnalysisPermissionChecker.getInstance(project)
    }

    @KaCachedService
    private val lifetimeTracker by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KaFirLifetimeTracker.getInstance(project)
    }

    @KaCachedService
    private val restrictedAnalysisService by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KotlinRestrictedAnalysisService.getInstance(project)
    }

    @KaCachedService
    private val tokenFactory by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KotlinLifetimeTokenFactory.getInstance(project)
    }

    @KaCachedService
    private val kotlinPlatformSettings by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KotlinPlatformSettings.getInstance(project)
    }

    private val writeActionStartedChecker = KaFirWriteActionStartedChecker(this)

    /**
     * [KaFirSession]s must be weakly referenced to allow simultaneous garbage collection of an unused [KaFirSession] together with its
     * [LLFirSession][org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession].
     *
     * The cache is deterministically cleaned up by a scheduled maintenance task, ensuring that cache entries for already garbage-collected
     * [KaFirSession]s will be removed even when the cache is not accessed. While the [KaFirSession] will have been garbage-collected, the
     * maintenance also frees up the strong [KaModule] key, which can hold references to PSI.
     */
    private val cache: Cache<KaModule, KaFirSession> = Caffeine.newBuilder().weakValues().build()

    private val lowMemoryWatcher: LowMemoryWatcher

    private val scheduledCacheMaintenance: Future<*>

    @KaCachedService
    private val cacheCleaner: KaFirCacheCleaner by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KaFirCacheCleaner.getInstance(project)
    }

    @KaCachedService
    private val analysisSessionStatistics: LLAnalysisSessionStatistics? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLStatisticsService.getInstance(project)?.analysisSessions
    }

    private val isSessionStructureLoggingEnabled by lazy(LazyThreadSafetyMode.PUBLICATION) {
        Registry.`is`("kotlin.analysis.sessionStructureLogging", false)
    }

    init {
        lowMemoryWatcher = LowMemoryWatcher.register(::handleLowMemoryEvent)
        scheduledCacheMaintenance = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
            { performCacheMaintenance() },
            10,
            10,
            TimeUnit.SECONDS,
        )
    }

    private fun performCacheMaintenance() {
        // This does NOT perform any invalidation, but rather removes cache entries for already garbage-collected sessions even when the
        // cache is not accessed for a while.
        cache.cleanUp()
    }

    /**
     * Checks that the [useSiteElement] is valid. Should be called before the use-site module is computed from the element, so that issues
     * with analysis on invalid PSI are caught as early as possible.
     */
    private fun checkUseSiteElement(useSiteElement: PsiElement) {
        PsiUtilCore.ensureValid(useSiteElement)
    }

    private fun checkUseSiteModule(useSiteModule: KaModule) {
        if (useSiteModule is KaLibraryModule && !kotlinPlatformSettings.allowUseSiteLibraryModuleAnalysis) {
            throw KaFirUseSiteLibraryModuleAnalysisException(useSiteModule)
        }

        requireWithAttachment(
            useSiteModule.isResolvable,
            { "`${useSiteModule::class.simpleName}` is not resolvable and thus cannot be a use-site module." },
        ) {
            withKaModuleEntry("useSiteModule", useSiteModule)
        }
    }

    /**
     * Checks whether analysis may be performed at all in the current context. Should be called before a session is acquired, so that we
     * don't create and cache a session for an analysis that is going to be rejected anyway.
     */
    private fun checkAnalysisAllowed() {
        if (!permissionChecker.isAnalysisAllowed()) {
            throw ProhibitedAnalysisException("Analysis is not allowed: ${permissionChecker.getRejectionReason()}")
        }

        ProgressManager.checkCanceled()

        restrictedAnalysisService?.run {
            if (isAnalysisRestricted && !isRestrictedAnalysisAllowed) {
                rejectRestrictedAnalysis()
            }
        }
    }

    private fun forEachListenerSafe(action: (KaSessionListener) -> Unit) = KaSessionListener.EP_NAME.forEachExtensionSafe(action)

    override fun getAnalysisSession(useSiteElement: PsiElement): KaSession {
        checkUseSiteElement(useSiteElement)

        val module = KotlinProjectStructureProvider.getModule(project, useSiteElement, useSiteModule = null)
        return acquireSessionWithListeners(module, useSiteElement)
    }

    override fun getAnalysisSession(useSiteModule: KaModule): KaSession {
        return acquireSessionWithListeners(useSiteModule, null)
    }

    private fun acquireSessionWithListeners(
        useSiteModule: KaModule,
        useSiteElement: PsiElement?,
    ): KaSession {
        forEachListenerSafe { it.beforeAcquiringSession(useSiteModule, useSiteElement) }

        return try {
            acquireAnalysisSession(useSiteModule)
        } catch (t: Throwable) {
            rethrowIntellijPlatformExceptionIfNeeded(t)
            forEachListenerSafe { it.onSessionAcquisitionException(useSiteModule, useSiteElement, t) }
            throw t
        } finally {
            forEachListenerSafe { it.afterAcquiringSession(useSiteModule, useSiteElement) }
        }
    }

    private fun acquireAnalysisSession(useSiteModule: KaModule): KaSession {
        // The checks must happen before the session is acquired, so that we don't create and cache a session for a rejected analysis.
        // They must also happen before `cacheCleaner.enterAnalysis()`, since an exception here doesn't call `cacheCleaner.exitAnalysis()`.
        checkAnalysisAllowed()
        checkUseSiteModule(useSiteModule)

        // The cache cleaner must be called before we get a session.
        // Otherwise, the acquired session might become invalid after the session cleanup.
        cacheCleaner.enterAnalysis()

        try {
            if (useSiteModule is KaDanglingFileModule && !useSiteModule.isStable) {
                return createAnalysisSession(useSiteModule)
            }

            val identifier = tokenFactory.identifier
            identifier.handleEnteringAnalysisInWriteAction(project)

            val session = cache.get(useSiteModule, ::createAnalysisSession)
                ?: error("`createAnalysisSession` must not return `null`.")

            checkSessionValidity(session)
            return session
        } catch (e: Throwable) {
            cacheCleaner.exitAnalysis()
            throw e
        }
    }

    private fun createAnalysisSession(useSiteKtModule: KaModule): KaFirSession {
        val resolutionFacade = useSiteKtModule.getResolutionFacade(project)
        val validityToken = tokenFactory.create(project, resolutionFacade.useSiteFirSession.createValidityTracker())
        return KaFirSession.createAnalysisSessionByResolutionFacade(resolutionFacade, validityToken)
    }

    private fun checkSessionValidity(session: KaFirSession) {
        requireWithAttachment(session.token.isValid(), { "An analysis session acquired via `getAnalysisSession` must be valid." }) {
            withKaModuleEntry("module", session.useSiteModule)
            withEntry("firSessionIsValid", session.firSession.isValid.toString())
            withEntry("firSessionInvalidationInformation", session.firSession.invalidationInformation)
        }
    }

    override fun beforeEnteringAnalysis(session: KaSession, useSiteElement: PsiElement) {
        try {
            analysisSessionStatistics?.analyzeCallCounter?.add(1)

            beforeEnteringAnalysisInternal(session, session.useSiteModule, useSiteElement)
        } catch (e: Throwable) {
            cacheCleaner.exitAnalysis()
            throw e
        }
    }

    override fun beforeEnteringAnalysis(session: KaSession, useSiteModule: KaModule) {
        try {
            analysisSessionStatistics?.analyzeCallCounter?.add(1)

            beforeEnteringAnalysisInternal(session, useSiteModule, null)
        } catch (e: Throwable) {
            cacheCleaner.exitAnalysis()
            throw e
        }
    }

    private fun beforeEnteringAnalysisInternal(session: KaSession, useSiteModule: KaModule, useSiteElement: PsiElement?) {
        // Session acquisition might take a while (e.g. when the session has to be created), so we check for cancellation again before
        // entering analysis.
        ProgressManager.checkCanceled()

        lifetimeTracker.beforeEnteringAnalysis(session)
        writeActionStartedChecker.beforeEnteringAnalysis()

        forEachListenerSafe { it.beforeEnteringAnalysis(useSiteModule, useSiteElement) }
    }

    override fun handleAnalysisException(throwable: Throwable, session: KaSession, useSiteElement: PsiElement): Nothing {
        handleAnalysisExceptionInternal(throwable, session.useSiteModule, useSiteElement)
    }

    override fun handleAnalysisException(throwable: Throwable, session: KaSession, useSiteModule: KaModule): Nothing {
        handleAnalysisExceptionInternal(throwable, useSiteModule, null)
    }

    private fun handleAnalysisExceptionInternal(
        throwable: Throwable,
        useSiteModule: KaModule,
        useSiteElement: PsiElement?,
    ): Nothing {
        rethrowIntellijPlatformExceptionIfNeeded(throwable)
        forEachListenerSafe { it.onAnalysisException(useSiteModule, useSiteElement, throwable) }

        if (restrictedAnalysisService?.isAnalysisRestricted == true && throwable !is Error) {
            throw KaFirRestrictedAnalysisException(cause = throwable)
        }

        throw throwable
    }

    override fun afterLeavingAnalysis(session: KaSession, useSiteElement: PsiElement) {
        afterLeavingAnalysisImpl { afterLeavingAnalysisInternal(session, session.useSiteModule, useSiteElement) }
    }

    override fun afterLeavingAnalysis(session: KaSession, useSiteModule: KaModule) {
        afterLeavingAnalysisImpl { afterLeavingAnalysisInternal(session, useSiteModule, null) }
    }

    private inline fun afterLeavingAnalysisImpl(action: () -> Unit) {
        try {
            try {
                action()
            } finally {
                tokenFactory.identifier.handleLeavingAnalysisInWriteAction(project)
            }
        } finally {
            cacheCleaner.exitAnalysis()
        }
    }

    private fun afterLeavingAnalysisInternal(session: KaSession, useSiteModule: KaModule, useSiteElement: PsiElement?) {
        forEachListenerSafe { it.afterLeavingAnalysis(useSiteModule, useSiteElement) }

        try {
            // `writeActionStartedChecker` might throw an "illegal write action" exception.
            writeActionStartedChecker.afterLeavingAnalysis()
        } finally {
            lifetimeTracker.afterLeavingAnalysis(session)
        }
    }

    /**
     * Schedules cache removal on a low-memory event.
     *
     * We ask the cache cleaner to schedule a global cache cleanup. It has to wait until there is no ongoing analysis as
     * [LLFirSessionCleaner][org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.cache.LLFirSessionCleaner] called from [LLFirSessionCache]
     * marks sessions invalid (and analyses in progress will be very surprised). [KaFirSession]s will also be invalidated during this step.
     */
    private fun handleLowMemoryEvent() {
        performCacheMaintenance()

        // We have to log the session structure before all caches (including sessions) are cleaned.
        logSessionStructure()

        cacheCleaner.scheduleCleanup()
    }

    /**
     * Writes the *current* session structure to a GraphML file if `kotlin.analysis.sessionStructureLogging`. This feature is only intended
     * for development purposes and can potentially lead to exceptions (e.g., during FIR element traversal). It must not be enabled in
     * production.
     *
     * See [LLSessionStructureWriter] for a guide about how to use the GraphML file. The file itself is written to IntelliJ's log folder, or
     * otherwise the log directory provided by [PathManager.getLogDir].
     */
    @OptIn(LLFirInternals::class)
    private fun logSessionStructure() {
        if (!isSessionStructureLoggingEnabled) return

        val sessionCacheStorage = LLFirSessionCache.getInstance(project).storage
        val analysisSessions = cache.asMap().values.toList()

        val logDir = PathManager.getLogDir().resolve("session-structure")
        Files.createDirectories(logDir)

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS"))
        val logFile = logDir.resolve("session-structure-$timestamp.graphml")

        // The cache traversal should NOT be executed in a read action. Otherwise, we might accidentally perform computations, which we want
        // to avoid when logging the *current* session structure.
        Files.newBufferedWriter(logFile).use { writer ->
            LLSessionStructureWriter.writeSessionStructure(
                storage = sessionCacheStorage,
                analysisRoots = analysisSessions.map { it.firSession },
                writer = writer,
            )
        }
    }

    override fun clearCaches() {
        cache.invalidateAll()
    }

    override fun dispose() {
        lowMemoryWatcher.stop()
        scheduledCacheMaintenance.cancel(false)
    }

    /**
     * Note: Races cannot happen because the cleanup is never performed concurrently.
     * See the KDoc for [LLFirSessionInvalidationTopics] for more information.
     */
    internal class SessionInvalidationListener(val project: Project) : LLFirSessionInvalidationListener {
        private val analysisSessionProvider: KaFirSessionProvider
            get() = getInstance(project) as? KaFirSessionProvider
                ?: error("Expected the analysis session provider to be a `${KaFirSessionProvider::class.simpleName}`.")

        override fun afterInvalidation(modules: Set<KaModule>) {
            val cache = analysisSessionProvider.cache
            modules.forEach { cache.invalidate(it) }
        }

        override fun afterGlobalInvalidation() {
            // Session invalidation events currently don't report whether library modules were included in the global invalidation. This is
            // by design to avoid iterating through the whole analysis session cache and to simplify the global session invalidation event.
            // Nevertheless, a `KaFirSession`'s validity is based on the underlying `LLFirSession`, so removed analysis sessions for
            // library modules might still be valid. This is not a problem, though, because analysis session caching is not required for
            // correctness, but rather a performance optimization.
            analysisSessionProvider.clearCaches()
        }
    }
}

private fun KClass<out KaLifetimeToken>.handleEnteringAnalysisInWriteAction(project: Project) {
    if (isAnalysisInWriteAction(this)) {
        // As we are inside a write action, we must flush deferred modifications to materialize its effects up to this point.
        LLFirDeclarationModificationService.getInstance(project).flushDeferredModifications()

        project.analysisMessageBus.syncPublisher(KotlinAnalysisInWriteActionListener.TOPIC).onEnteringAnalysisInWriteAction()
    }
}

private fun KClass<out KaLifetimeToken>.handleLeavingAnalysisInWriteAction(project: Project) {
    if (isAnalysisInWriteAction(this)) {
        project.analysisMessageBus.syncPublisher(KotlinAnalysisInWriteActionListener.TOPIC).afterLeavingAnalysisInWriteAction()
    }
}

private fun isAnalysisInWriteAction(lifetimeTokenIdentifier: KClass<out KaLifetimeToken>): Boolean =
    lifetimeTokenIdentifier == KotlinReadActionConfinementLifetimeToken::class &&
            KaAnalysisPermissionRegistry.getInstance().isAnalysisAllowedInWriteAction &&
            ApplicationManager.getApplication().isWriteAccessAllowed

private class ProhibitedAnalysisException(override val message: String) : IllegalStateException()
