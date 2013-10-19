/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.resolve.calls.tasks.ResolutionCandidate;

public enum RecursionStatus {
    MIGHT_BE(true),
    FOUND_IN_RETURN(true),
    FOUND_IN_FINALLY(true),
    FOUND_IN_RETURN_IN_FINALLY(true),
    NO_TAIL(false);

    private final boolean doGenerateTailRecursion;

    RecursionStatus(boolean doGenerateTailRecursion) {
        this.doGenerateTailRecursion = doGenerateTailRecursion;
    }

    public boolean isDoGenerateTailRecursion() {
        return doGenerateTailRecursion;
    }

    public RecursionStatus and(RecursionStatus b) {
        if (this == b) {
            return this;
        }

        if (isOneOf(this, b, NO_TAIL)) {
            return NO_TAIL;
        }
        if (isOneOf(this, b, FOUND_IN_RETURN_IN_FINALLY)) {
            return FOUND_IN_RETURN_IN_FINALLY;
        }
        if (isCase(this, b, FOUND_IN_RETURN, FOUND_IN_FINALLY)) {
            return FOUND_IN_RETURN_IN_FINALLY;
        }
        if (isOneOf(this, b, FOUND_IN_RETURN)) {
            return FOUND_IN_RETURN;
        }
        if (isOneOf(this, b, FOUND_IN_FINALLY)) {
            return FOUND_IN_FINALLY;
        }

        return this;
    }

    private static boolean isOneOf(RecursionStatus a, RecursionStatus b, RecursionStatus value) {
        return a == value || b == value;
    }

    private static boolean isCase(RecursionStatus a, RecursionStatus b, RecursionStatus value1, RecursionStatus value2) {
        return (a == value1 && b == value2) || (a == value2 && b == value1);
    }
}
