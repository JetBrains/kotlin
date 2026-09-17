/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.events

import org.jetbrains.kotlin.buildtools.api.trackers.IcEvent
import java.io.Serializable

// Ideally, the basic interface should be in shared bta api folder, while the concrete implementations should be in kotlin-build-statistics

sealed class IcEventImpl(
    override val type: String = this::class.simpleName.toString(),
    override val severity: String,
) : IcEvent, Serializable {
    final override val timestamp: Long = System.currentTimeMillis()
    override var iteration: Int = UNASSIGNED_ITERATION

    class CompileIteration(
        override val files: List<String>,
        override val exitCode: String
    ) : IcEventImpl("CompileIteration", "NONE"),
        IcEvent.CompileIteration { val serialVersionUID: Long = 0L }

    class SourceChanges(
        override val changeInfo: String,
        override val modifiedFiles: List<String>,
        override val deletedFiles: List<String>,
    ) : IcEventImpl("SourceChanges", "DEBUG"),
        IcEvent.SourceChanges { val serialVersionUID: Long = 0L }

    class ConfigInputs(
        override val icConfiguration: Map<String, String?>,
        override val compilerArguments: List<String>
    ) : IcEventImpl("ConfigInputs", "DEBUG"),
        IcEvent.ConfigInputs { val serialVersionUID: Long = 0L }

    class CleaningOutputDirs(
        override val outputDirs: List<String>
    ) : IcEventImpl("CleaningOutputDirs", "DEBUG"),
        IcEvent.CleaningOutputDirs { val serialVersionUID: Long = 0L }

    companion object {
        const val serialVersionUID: Long = 0L
        const val UNASSIGNED_ITERATION: Int = -1

        const val NONE = "NONE"
        const val WARNING = "WARNING"
        const val INFO = "INFO"
        const val DEBUG = "DEBUG"
    }
}
