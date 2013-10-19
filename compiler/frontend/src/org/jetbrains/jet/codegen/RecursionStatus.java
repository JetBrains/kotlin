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

public enum RecursionStatus {
    MIGHT_BE(true, false),
    FOUND_IN_RETURN(true, true),
    FOUND_IN_FINALLY(false, false),
    NO_TAIL(false, false);

    private final boolean doGenerateTailRecursion;
    private final boolean isReturn;

    RecursionStatus(boolean doGenerateTailRecursion, boolean aReturn) {
        this.doGenerateTailRecursion = doGenerateTailRecursion;
        isReturn = aReturn;
    }

    public boolean isDoGenerateTailRecursion() {
        return doGenerateTailRecursion;
    }

    public boolean isReturn() {
        return isReturn;
    }

    public RecursionStatus and(RecursionStatus b) {
        if (this == b) {
            return this;
        }
        if (!this.isDoGenerateTailRecursion()) {
            return this;
        }
        if (!b.isDoGenerateTailRecursion()) {
            return this;
        }

        if (isOneOf(this, b, FOUND_IN_RETURN)) {
            return FOUND_IN_RETURN;
        }

        return this;
    }

    private static boolean isOneOf(RecursionStatus a, RecursionStatus b, RecursionStatus value) {
        return a == value || b == value;
    }

}
