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

package org.jetbrains.kotlin.parsing;

public class LastBefore extends AbstractTokenStreamPattern {
    private final boolean dontStopRightAfterOccurrence;
    private final TokenStreamPredicate lookFor;
    private final TokenStreamPredicate stopAt;

    private boolean previousLookForResult;

    public LastBefore(TokenStreamPredicate lookFor, TokenStreamPredicate stopAt, boolean dontStopRightAfterOccurrence) {
        this.lookFor = lookFor;
        this.stopAt = stopAt;
        this.dontStopRightAfterOccurrence = dontStopRightAfterOccurrence;
    }

    public LastBefore(TokenStreamPredicate lookFor, TokenStreamPredicate stopAt) {
        this(lookFor, stopAt, false);
    }

    @Override
    public boolean processToken(int offset, boolean topLevel) {
        boolean lookForResult = lookFor.matching(topLevel);
        if (lookForResult) {
            lastOccurrence = offset;
        }
        if (stopAt.matching(topLevel)) {
            if (topLevel
                && (!dontStopRightAfterOccurrence
                    || !previousLookForResult)) return true;
        }
        previousLookForResult = lookForResult;
        return false;
    }
}
