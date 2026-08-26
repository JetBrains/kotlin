/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.io

import java.io.Serializable

enum class IcEventType {

    // Standard ICReporter method events

    IC_ITERATION,
    DIRTY_FILE,
    DIRTY_CLASS,
    DIRTY_MEMBER,

    // IncrementalCompilerRunner.kt

    SRC_CHANGES,
    CONFIG_INPUTS,

    IC_COMPLETED,
    IC_FAILED,

    WITH_JAR_SNAPSHOT,
    NO_JAR_SNAPSHOT,

    CLEANING_OUTPUT_DIRS,
    OUTPUT_DIR_EMPTY,
    NO_OUTPUT_ITEM,

    // buildUtil.kt

    PROCESSING_CHANGE,
}

sealed class IcEvent(
    val type: IcEventType,
    val iteration: Int,
    val severity: String,
    val timestamp: Long = System.currentTimeMillis(),
) : Serializable {
    abstract val readableString: String

    class BasicICEvent(
        type: IcEventType,
        iteration: Int,
        severity: String
    ) : IcEvent(type, iteration, severity) {
        override val readableString: String
            get() = "$type"
    }

    class CompileIteration(
        iteration: Int,
        val files: List<String>,
        val exitCode: String
    ) : IcEvent(IcEventType.IC_ITERATION, iteration, "NONE") {
        override val readableString: String
            get() = "IC iteration $iteration completed with exit code $exitCode"
    }

    class SourceChanges(
        iteration: Int,
        val changeInfo: String,
        val modifiedFiles: List<String>,
        val deletedFiles: List<String>,
    ) : IcEvent(IcEventType.SRC_CHANGES, iteration, "DEBUG") {
        override val readableString: String
            get() = "Source changes: $changeInfo"

        private companion object {
            const val serialVersionUID = 0L
        }
    }

    class ConfigInputs(
        iteration: Int,
        val icConfigurationInputsSnapshot: Map<String, String?>,
        val compilerArgumentsInputsSnapshot: List<String>
    ) : IcEvent(IcEventType.CONFIG_INPUTS, iteration, "DEBUG") {
        override val readableString: String
            get() = "Configuration inputs: $compilerArgumentsInputsSnapshot, $icConfigurationInputsSnapshot"
    }

    class CleaningOutputDirs(
        iteration: Int,
        val outputDirs: List<String>
    ) : IcEvent(IcEventType.CLEANING_OUTPUT_DIRS, iteration, "DEBUG") {
        override val readableString: String
            get() = "Cleaning output directories with total size ${outputDirs.size}"
    }

    companion object { const val serialVersionUID = 0L }  // and one per subclass
}
