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

package org.jetbrains.jet;

import jet.runtime.ProgressionUtil;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class ProgressionUtilTest {
    private static final int MAX = Integer.MAX_VALUE;
    private static final int MIN = Integer.MIN_VALUE;

    private static void doTest(int start, int end, int increment, int expected) {
        int actualInt = ProgressionUtil.getProgressionFinalElement(start, end, increment);
        assertEquals(expected, actualInt);

        long actualLong = ProgressionUtil.getProgressionFinalElement((long) start, (long) end, (long) increment);
        assertEquals(expected, actualLong);
    }

    private static final int[] INTERESTING = new int[]{ MIN, MIN / 2, -239, -23, -1, 0, 1, 42, 239, MAX / 2, MAX };

    @Test
    public void testGetFinalElement() {
        // start == end
        for (int x : INTERESTING) {
            for (int increment : INTERESTING) if (increment != 0) {
                doTest(x, x, increment, x);
            }
        }

        // increment == 1
        for (int start = 0; start < INTERESTING.length; start++) {
            for (int end = start; end < INTERESTING.length; end++) {
                doTest(INTERESTING[start], INTERESTING[end], 1, INTERESTING[end]);
            }
        }

        // increment == -1
        for (int end = 0; end < INTERESTING.length; end++) {
            for (int start = end; start < INTERESTING.length; start++) {
                doTest(INTERESTING[start], INTERESTING[end], -1, INTERESTING[end]);
            }
        }

        // end == MAX
        doTest(0, MAX, MAX, MAX);
        doTest(0, MAX, MAX / 2, MAX - 1);
        doTest(MIN + 1, MAX, MAX, MAX);
        doTest(MAX - 7, MAX, 3, MAX - 1);
        doTest(MAX - 7, MAX, MAX, MAX - 7);

        // end == MIN
        doTest(0, MIN, MIN, MIN);
        doTest(0, MIN, MIN / 2, MIN);
        doTest(MAX, MIN, MIN, -1);
        doTest(MIN + 7, MIN, -3, MIN + 1);
        doTest(MIN + 7, MIN, MIN, MIN + 7);

        // Small tests
        for (int start = -10; start < 10; start++) {
            for (int end = -10; end < 10; end++) {
                for (int increment = -20; increment < 20; increment++) {
                    // Cut down incorrect test data
                    if (increment == 0) continue;
                    if ((increment > 0) != (start <= end)) continue;

                    // Iterate over the progression and obtain the expected result
                    int x = start;
                    while (true) {
                        int next = x + increment;
                        if (next < Math.min(start, end) || next > Math.max(start, end)) break;
                        x = next;
                    }

                    doTest(start, end, increment, x);
                }
            }
        }
    }
}
