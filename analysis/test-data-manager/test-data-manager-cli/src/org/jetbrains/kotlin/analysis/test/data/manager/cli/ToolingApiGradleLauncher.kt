/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.cli

import org.gradle.tooling.*
import org.gradle.tooling.events.*
import org.gradle.tooling.events.ProgressEvent
import org.gradle.tooling.events.ProgressListener
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import java.io.OutputStream
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Runs builds through the Gradle Tooling API, with the Gradle of the wrapper and the daemons `./gradlew` uses.
 *
 * Unlike a `gradlew` process, the Tooling API reports the failures as data: which tasks failed with what, and the
 * failures of the build itself, so they aren't read from the console output. It also cancels the build on request on any
 * platform.
 *
 * The tasks go into the arguments, as on the command line: `forTasks` would be replaced by any task among them.
 */
internal class ToolingApiGradleLauncher(private val root: Path) : GradleLauncher {
    override fun start(arguments: List<String>, output: OutputStream): RunningBuild {
        val result = CompletableFuture<GradleOutcome?>()
        val cancellation = GradleConnector.newCancellationTokenSource()
        val listener = FailureListener()
        var connection: ProjectConnection? = null
        try {
            connection = GradleConnector.newConnector().forProjectDirectory(root.toFile()).connect()
            connection.newBuild()
                .withArguments(arguments)
                .setStandardOutput(output)
                .setStandardError(output)
                .setColorOutput(false)
                .withCancellationToken(cancellation.token())
                .addProgressListener(listener, OperationType.TASK, OperationType.ROOT)
                .run(object : ResultHandler<Void?> {
                    override fun onComplete(ignored: Void?) {
                        result.complete(GradleOutcome(failed = false, failures = listener.failures(exception = null)))
                    }

                    override fun onFailure(failure: GradleConnectionException) {
                        result.complete(GradleOutcome(failed = true, failures = listener.failures(failure)))
                    }
                })
        } catch (e: Exception) {
            // Reading the wrapper properties, for one, fails right away
            connection?.close()
            connection = null
            val launchFailure = GradleFailure(taskPath = null, renderFailure(e.toNode()))
            result.complete(GradleOutcome(failed = true, failures = listOf(launchFailure)))
        }

        val startedConnection = connection
        return object : RunningBuild {
            override fun cancel() {
                cancellation.cancel()
            }

            override fun abandon() {
                result.complete(null)
            }

            // An abandoned build keeps the connection, whose closing would wait for the build: the process ends instead
            override fun await(): GradleOutcome? = result.get()?.also { startedConnection?.close() }
        }
    }

    /** Collects the failures of the tasks and of the build from the progress events. */
    private class FailureListener : ProgressListener {
        private val taskFailures = ConcurrentLinkedQueue<Pair<String, FailureNode>>()

        @Volatile
        private var buildFailures: List<FailureNode> = emptyList()

        override fun statusChanged(event: ProgressEvent) {
            if (event is TaskFinishEvent) {
                val result = event.result as? TaskFailureResult ?: return
                result.failures.mapTo(taskFailures) { event.descriptor.taskPath to it.toNode() }
            } else if (event is FinishEvent && event.descriptor.parent == null) {
                buildFailures = (event.result as? FailureResult)?.failures.orEmpty().map { it.toNode() }
            }
        }

        /**
         * The failures of the build, see [collectFailures]. The [exception] the build ended with counts unless it is a
         * cancellation, or a failure of the build the events already have; one without such events, like an unknown option or
         * a daemon that disappeared, counts.
         */
        fun failures(exception: GradleConnectionException?): List<GradleFailure> {
            val tasks = taskFailures.toList()
            val build = buildFailures
            val reported = tasks.isNotEmpty() || build.isNotEmpty()
            val launchFailure = exception?.takeIf { it !is BuildCancelledException && !(reported && it is BuildException) }
            return collectFailures(tasks, build, launchFailure?.toNode())
        }
    }
}

private fun Failure.toNode(): FailureNode = FailureNode(message, causes.map { it.toNode() })

private fun Throwable.toNode(): FailureNode = FailureNode(message, listOfNotNull(cause?.takeIf { it !== this }?.toNode()))
