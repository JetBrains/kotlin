/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.continuous

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Continuously recompiles a Kotlin/JVM compilation unit: every time source files change, an incremental compilation round is run,
 * so the destination directory is always up-to-date.
 *
 * Changes are either pushed by the client via [submitChanges] (e.g., by an IDE on file save), or detected by the built-in watcher
 * started via [watch]. Changes arriving within [debounceMillis] are compiled in a single round, and rounds never overlap.
 * Compilations are executed within the given [session], so the state kept by the session (e.g., the compiler application environment
 * for the in-process execution) stays warm between the rounds.
 *
 * Combined with the error tolerance compiler plugin, this gives an Eclipse-like experience: code with errors is compiled into
 * `throw java.lang.Error(...)`, and the rest of the program can be run as usual.
 *
 * Usage example:
 * ```
 * val toolchains = KotlinToolchains.loadImplementation(implementationClasspath)
 * toolchains.createBuildSession().use { session ->
 *     ContinuousJvmCompilation(session, toolchains.createInProcessExecutionPolicy(), sources, createOperation = { sources, changes ->
 *         toolchains.jvm.jvmCompilationOperationBuilder(sources, destination).apply {
 *             compilerArguments.applyCommandLineArguments(listOf("-Xplugin=$errorTolerancePluginJar"))
 *             this[JvmCompilationOperation.INCREMENTAL_COMPILATION] =
 *                 snapshotBasedIcConfigurationBuilder(icWorkingDirectory, changes, dependenciesSnapshotFiles = emptyList()).build()
 *         }.build()
 *     }).use { compilation ->
 *         compilation.start()
 *         compilation.watch(listOf(sourceRoot))
 *         // ...
 *     }
 * }
 * ```
 *
 * @param sources all sources of the compilation unit at the moment of the creation; new and removed files are tracked afterward
 * @param createOperation creates a compilation operation for the round, it's expected to configure snapshot-based incremental compilation
 *  with the given [SourcesChanges]
 */
@ExperimentalBuildToolsApi
public class ContinuousJvmCompilation(
    private val session: KotlinToolchains.BuildSession,
    private val executionPolicy: ExecutionPolicy,
    sources: Collection<Path>,
    private val createOperation: (sources: List<Path>, sourcesChanges: SourcesChanges) -> JvmCompilationOperation,
    private val listener: Listener = Listener {},
    private val logger: KotlinLogger? = null,
    private val debounceMillis: Long = 100,
) : AutoCloseable {
    /**
     * A finished compilation round.
     *
     * @property sourcesChanges the changes which were compiled during the round
     * @property failure an exception thrown during the round, if any (in this case [result] is [CompilationResult.COMPILER_INTERNAL_ERROR])
     */
    public class Round(
        public val result: CompilationResult,
        public val sourcesChanges: SourcesChanges,
        public val durationMillis: Long,
        public val failure: Throwable?,
    )

    public fun interface Listener {
        /**
         * Called on the compilation thread after each round.
         */
        public fun onRoundFinished(round: Round)
    }

    private val lock = ReentrantLock()
    private val idle = lock.newCondition()
    private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "Kotlin continuous compilation").apply { isDaemon = true }
    }

    // All the fields below are guarded by the lock
    private val currentSources = LinkedHashSet<Path>(sources.map { it.toAbsolutePath().normalize() })
    private val pendingModified = LinkedHashSet<Path>()
    private val pendingRemoved = LinkedHashSet<Path>()
    private var resyncRequired = false
    private var scheduledRound: ScheduledFuture<*>? = null
    private var roundInProgress = false
    private var closed = false
    private var watcher: PollingSourceWatcher? = null

    /**
     * Runs the initial round, which lets incremental compilation detect the changes made since the previous compilation.
     */
    public fun start() {
        lock.withLock {
            resyncRequired = true
            scheduleRound(delayMillis = 0)
        }
    }

    /**
     * Notifies about changed source files. Files which aren't known yet are added to the compilation unit.
     */
    public fun submitChanges(modified: Collection<Path>, removed: Collection<Path> = emptyList()) {
        if (modified.isEmpty() && removed.isEmpty()) return
        lock.withLock {
            check(!closed) { "Continuous compilation is closed" }
            for (path in removed.map { it.toAbsolutePath().normalize() }) {
                pendingModified.remove(path)
                if (currentSources.remove(path)) pendingRemoved.add(path)
            }
            for (path in modified.map { it.toAbsolutePath().normalize() }) {
                pendingRemoved.remove(path)
                pendingModified.add(path)
                currentSources.add(path)
            }
            scheduleRound(debounceMillis)
        }
    }

    /**
     * Starts watching [sourceRoots] for changes of files with the given [extensions]. Detected changes are passed to [submitChanges].
     *
     * The watcher polls the file system, which is portable and reliable (unlike `java.nio.file.WatchService` on macOS), but walks all the
     * source roots every [pollIntervalMillis].
     */
    public fun watch(sourceRoots: List<Path>, extensions: Set<String> = setOf("kt", "java"), pollIntervalMillis: Long = 200) {
        lock.withLock {
            check(!closed) { "Continuous compilation is closed" }
            check(watcher == null) { "Already watching" }
            watcher = PollingSourceWatcher(sourceRoots, extensions, pollIntervalMillis) { modified, removed ->
                submitChanges(modified, removed)
            }.also { it.start() }
        }
    }

    /**
     * Waits until all the submitted changes are compiled.
     *
     * @return `false` if the timeout elapsed before that
     */
    public fun awaitIdle(timeoutMillis: Long): Boolean {
        var remainingNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        lock.withLock {
            while (roundInProgress || scheduledRound != null) {
                if (remainingNanos <= 0) return false
                remainingNanos = idle.awaitNanos(remainingNanos)
            }
            return true
        }
    }

    override fun close() {
        lock.withLock {
            if (closed) return
            closed = true
            watcher?.close()
            scheduledRound?.cancel(false)
        }
        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.MINUTES)
    }

    private fun scheduleRound(delayMillis: Long) {
        if (closed) return
        scheduledRound?.cancel(false)
        scheduledRound = executor.schedule(::runRound, delayMillis, TimeUnit.MILLISECONDS)
    }

    private fun runRound() {
        val sources: List<Path>
        val changes: SourcesChanges
        lock.withLock {
            scheduledRound = null
            if (!resyncRequired && pendingModified.isEmpty() && pendingRemoved.isEmpty()) {
                idle.signalAll()
                return
            }
            sources = currentSources.toList()
            changes = when {
                resyncRequired -> SourcesChanges.ToBeCalculated
                else -> SourcesChanges.Known(pendingModified.map { it.toFile() }, pendingRemoved.map { it.toFile() })
            }
            pendingModified.clear()
            pendingRemoved.clear()
            resyncRequired = false
            roundInProgress = true
        }

        val start = System.nanoTime()
        val round = try {
            val result = session.executeOperation(createOperation(sources, changes), executionPolicy, logger)
            Round(result, changes, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), failure = null)
        } catch (e: Throwable) {
            logger?.error("Continuous compilation round failed", e)
            // The state of the incremental compilation is unknown, so the next round should detect the changes itself
            lock.withLock { resyncRequired = true }
            Round(CompilationResult.COMPILER_INTERNAL_ERROR, changes, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), e)
        }

        try {
            listener.onRoundFinished(round)
        } catch (e: Throwable) {
            logger?.error("Continuous compilation listener failed", e)
        } finally {
            lock.withLock {
                roundInProgress = false
                if (pendingModified.isNotEmpty() || pendingRemoved.isNotEmpty()) {
                    if (scheduledRound == null) scheduleRound(debounceMillis)
                } else if (scheduledRound == null) {
                    idle.signalAll()
                }
            }
        }
    }
}
