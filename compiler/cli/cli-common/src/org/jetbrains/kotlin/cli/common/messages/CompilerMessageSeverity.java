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

import java.util.EnumSet;

public enum CompilerMessageSeverity {
    ERROR(0),
    EXCEPTION(5),
    WARNING(10),
    INFO(15),
    OUTPUT(20),
    LOGGING(25);

    private final int value;

    CompilerMessageSeverity(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    static CompilerMessageSeverity fromValue(int value) {
        for (CompilerMessageSeverity severity : CompilerMessageSeverity.values()) {
            if (severity.value == value) return severity;
        }

        return null;
    }

    public static final EnumSet<CompilerMessageSeverity> ERRORS = EnumSet.of(ERROR, EXCEPTION);
    public static final EnumSet<CompilerMessageSeverity> VERBOSE = EnumSet.of(OUTPUT, LOGGING);

    public boolean isError() {
        return ERRORS.contains(this);
    }
}
