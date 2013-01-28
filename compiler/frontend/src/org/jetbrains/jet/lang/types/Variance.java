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

package org.jetbrains.jet.lang.types;

public enum Variance {
    INVARIANT("", true, true, 0),
    IN_VARIANCE("in", true, false, -1),
    OUT_VARIANCE("out", false, true, +1);

    private final String label;
    private final boolean allowsInPosition;
    private final boolean allowsOutPosition;
    private final int superpositionFactor;

    Variance(String label, boolean allowsInPosition, boolean allowsOutPosition, int superpositionFactor) {
        this.label = label;
        this.allowsInPosition = allowsInPosition;
        this.allowsOutPosition = allowsOutPosition;
        this.superpositionFactor = superpositionFactor;
    }

    public boolean allowsInPosition() {
        return allowsInPosition;
    }

    public boolean allowsOutPosition() {
        return allowsOutPosition;
    }

    public Variance superpose(Variance other) {
        int r = this.superpositionFactor * other.superpositionFactor;
        switch (r) {
            case  0: return INVARIANT;
            case -1: return IN_VARIANCE;
            case +1: return OUT_VARIANCE;
        }
        throw new IllegalStateException();
    }

    public Variance opposite() {
        switch (this) {
            case INVARIANT:
                return INVARIANT;
            case IN_VARIANCE:
                return OUT_VARIANCE;
            case OUT_VARIANCE:
                return IN_VARIANCE;
        }
        throw new IllegalStateException("Impossible variance: " + this);
    }

    @Override
    public String toString() {
        return label;
    }
}
