/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.common.messages;

import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public enum CompilerMessageSeverity {
    EXCEPTION,
    ERROR,
    // Unlike a normal warning, a strong warning is not discarded when there are compilation errors.
    // Use it for problems related to configuration, not the diagnostics
    STRONG_WARNING,
    WARNING,
    INFO,
    LOGGING,
    /**
     * Source to output files mapping messages (e.g A.kt->A.class).
     * It is needed for incremental compilation.
     */
    OUTPUT;

    public static final EnumSet<CompilerMessageSeverity> VERBOSE = EnumSet.of(LOGGING);

    public boolean isError() {
        return this == EXCEPTION || this == ERROR;
    }

    public boolean isWarning() {
        return this == STRONG_WARNING || this == WARNING;
    }

    @NotNull
    public String getPresentableName() {
        switch (this) {
            case EXCEPTION:
                return "exception";
            case ERROR:
                return "error";
            case STRONG_WARNING:
            case WARNING:
                return "warning";
            case INFO:
                return "info";
            case LOGGING:
                return "logging";
            case OUTPUT:
                return "output";
            default:
                throw new UnsupportedOperationException("Unknown severity: " + this);
        }
    }
}
