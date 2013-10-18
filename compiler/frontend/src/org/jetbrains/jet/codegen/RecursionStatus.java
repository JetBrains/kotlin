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
    MIGHT_BE,
    FOUND_IN_RETURN,
    NO_TAIL;

    public RecursionStatus and(RecursionStatus b) {
        if (this == b) {
            return this;
        }

        switch (this) {
            case NO_TAIL:
                return NO_TAIL;
            case FOUND_IN_RETURN:
                return FOUND_IN_RETURN;
            case MIGHT_BE:
                return b.and(this);
            default:
                throw new UnsupportedOperationException(this.toString());
        }
    }
}
