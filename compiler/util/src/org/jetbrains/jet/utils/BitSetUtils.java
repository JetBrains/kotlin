/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.utils;

import java.util.BitSet;

/**
 * @author Evgeny Gerashchenko
 * @since 6/14/12
 */
public class BitSetUtils {
    private BitSetUtils() {
    }

    public static int toInt(BitSet bitSet) {
        int intValue = 0;
        for (int bit = 0; bit < bitSet.length(); bit++) {
            if (bitSet.get(bit)) {
                intValue |= (1 << bit);
            }
        }
        return intValue;
    }

    public static BitSet toBitSet(int value) {
        BitSet bitSet = new BitSet();
        int bit = 0;
        while (value > 0) {
            bitSet.set(bit++, value % 2 == 1);
            value >>= 1;
        }
        return bitSet;
    }
}
