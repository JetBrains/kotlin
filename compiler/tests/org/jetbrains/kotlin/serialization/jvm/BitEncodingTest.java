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

package org.jetbrains.kotlin.serialization.jvm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase;

import java.util.Random;

import static org.jetbrains.kotlin.serialization.jvm.BitEncoding.decodeBytes;
import static org.jetbrains.kotlin.serialization.jvm.BitEncoding.encodeBytes;
import static org.junit.Assert.assertArrayEquals;

public class BitEncodingTest extends KtUsefulTestCase {
    private static final int[] BIG_LENGTHS = new int[]
            {1000, 32000, 33000, 65000, 65534, 65535, 65536, 65537, 100000, 131074, 239017, 314159, 1000000};

    private static void doTest(int randSeed, int length) throws Exception {
        byte[] a = new byte[length];
        new Random(randSeed).nextBytes(a);

        String[] b = encodeBytes(a);
        for (String string : b) {
            assertStringConformsToJVMS(string);
        }

        byte[] c = decodeBytes(b);
        String message = "Failed randSeed = " + randSeed + ", length = " + length;
        assertArrayEquals(message, a, c);

        String[] d = encodeBytes(c);
        assertArrayEquals(message, b, d);

        byte[] e = decodeBytes(d);
        assertArrayEquals(message, a, e);

    }

    private static void assertStringConformsToJVMS(@NotNull String string) {
        int effectiveLength = string.length();
        for (char c : string.toCharArray()) {
            if (c == 0x0) effectiveLength++;
        }
        assertTrue(String.format("String exceeds maximum allowed length in a class file: %d > 65535", effectiveLength),
                   effectiveLength <= 65535);
    }

    public void testEncodeDecode() throws Exception {
        for (int length = 0; length <= 100; length++) {
            for (int randSeed = 1; randSeed <= 100; randSeed++) {
                doTest(randSeed, length);
            }
        }

        for (int length : BIG_LENGTHS) {
            for (int randSeed = 1; randSeed <= 3; randSeed++) {
                doTest(randSeed, length);
            }
        }
    }
}
