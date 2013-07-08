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

package org.jetbrains.jet.descriptors.serialization;

import com.intellij.testFramework.UsefulTestCase;

import java.util.Random;

import static org.jetbrains.jet.descriptors.serialization.JavaProtoBufUtil.decodeBytes;
import static org.jetbrains.jet.descriptors.serialization.JavaProtoBufUtil.encodeBytes;
import static org.junit.Assert.assertArrayEquals;

public class JavaProtoBufUtilTest extends UsefulTestCase {
    private static final int[] LENGTHS = new int[] {0, 1, 2, 3, 7, 10, 100, 1000, 32000, 33000, 65000, 65534, 65535, 65536, 65537,
            100000, 131074, 239017, 314159, 1000000};

    private static void doTest(int randSeed, int length) {
        byte[] a = new byte[length];
        new Random(randSeed).nextBytes(a);

        String message = "Failed randSeed = " + randSeed + ", length = " + length;

        String[] b = encodeBytes(a);
        byte[] c = decodeBytes(b);
        assertArrayEquals(message, a, c);

        String[] d = encodeBytes(c);
        assertArrayEquals(message, d, b);

        byte[] e = decodeBytes(d);
        assertArrayEquals(message, a, e);
    }

    public void testEncodeDecode() {
        for (int randSeed = 1; randSeed <= 3; randSeed++) {
            for (int length : LENGTHS) {
                doTest(randSeed, length);
            }
        }
    }
}
