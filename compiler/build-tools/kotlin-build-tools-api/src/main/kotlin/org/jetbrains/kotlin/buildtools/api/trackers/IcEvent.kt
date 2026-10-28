/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.trackers

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * Carries data about compiler decisions that happen during the Incremental Compilation (IC) process.
 * Serves as a transport method between BTA-impl and its caller.
 *
 * Currently only used to report incremental compilation attempts running through `IncrementalCompilerRunner`.
 *
 * Each event captures the following common properties:
 * - [type]: String representing type of the event (which determines what data it carries).
 * - [timestamp]: Timestamp indicating when the event was recorded.
 * - [iteration]: Sequence number of the compilation iteration during which the event occurred (counted from 0).
 */
@ExperimentalBuildToolsApi
public interface IcEvent {
    public val type: String
    public val timestamp: Long
    public val iteration: Int

    /**
     * Used to indicate a start of a compilation round.
     *
     * - [isIncremental]: Boolean indicating whether the given compilation is incremental.
     * - [reason]: String representing the reason for why the given compilation is not incremental.
     */
    public interface CompilationStarted : IcEvent {
        public val isIncremental: Boolean
        public val reason: String?
    }

    /**
     * Emitted at the end of each compilation round.
     *
     * - [reasons]: Map from each file compiled in the given compilation round to a list
     *              of human-readable reasons for why it was recompiled.
     * - [exitCode]: String representation of the `ExitCode` emitted at the end of the given compilation round.
     */
    public interface CompileIteration : IcEvent {
        public val reasons: Map<String, List<FileCompilationReason>>
        public val exitCode: String
    }
}
