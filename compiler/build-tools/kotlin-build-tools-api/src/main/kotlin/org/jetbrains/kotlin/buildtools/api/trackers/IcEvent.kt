/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.trackers

public interface IcEvent {
    public val type: String
    public val severity: String
    public val timestamp: Long
    public val iteration: Int

    public interface CompileIteration : IcEvent {
        public val files: List<String>
        public val exitCode: String
    }

    public interface SourceChanges : IcEvent {
        public val changeInfo: String
        public val modifiedFiles: List<String>
        public val deletedFiles: List<String>
    }

    public interface ConfigInputs : IcEvent {
        public val icConfiguration: Map<String, String?>
        public val compilerArguments: List<String>
    }

    public interface CleaningOutputDirs : IcEvent {
        public val outputDirs: List<String>
    }
}
