/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.cli

import java.io.OutputStream

/** Starts Gradle builds of the repository. */
internal fun interface GradleLauncher {
    /**
     * Starts a build with the command line [arguments], tasks included, its standard output and error going to [output].
     * A launch that fails gives a [RunningBuild] that has already failed.
     */
    fun start(arguments: List<String>, output: OutputStream): RunningBuild
}

/** A build [GradleLauncher] started. */
internal interface RunningBuild {
    /** Asks Gradle to cancel the build; [await] still waits for it to stop. */
    fun cancel()

    /** Makes [await] return `null` at once, leaving the build to Gradle. */
    fun abandon()

    /** Waits for the build to finish; `null` if it was [abandon]ed. */
    fun await(): GradleOutcome?
}

/**
 * How a build ended: whether it [failed], and its [failures], what Gradle lists under `What went wrong` in the console.
 */
internal class GradleOutcome(val failed: Boolean, val failures: List<GradleFailure>)

/**
 * A failure of a build: of the task [taskPath], or of the build itself (its configuration, an unknown task, the launch),
 * rendered as [text] the way the console shows it.
 */
internal class GradleFailure(val taskPath: String?, val text: String)
