/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.trackers

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

@ExperimentalBuildToolsApi
public interface IcEvent {
    public val type: String
    public val severity: String
    public val timestamp: Long
    public val iteration: Int

    public interface CompilationStarted : IcEvent {
        public val isIncremental: Boolean
        public val reason: String?
    }

    public interface CompileIteration : IcEvent {
        public val files: List<String>
        public val reasons: Map<String, List<String>>
        public val exitCode: String
    }

    public interface Unknown : IcEvent {
        public val unknownType: String
    }
}
