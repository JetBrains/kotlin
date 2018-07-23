/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.text.harmony_regex

import kotlin.text.*
import kotlin.test.*

class MatchResultTest2 {

    @Test fun testErrorConditions2() {
        // Test match cursors in absence of a match
        val regex = Regex("(foo[0-9])(bar[a-z])")
        var result = regex.find("foo1barzfoo2baryfoozbar5")

        assertTrue(result != null)
        assertEquals(0, result!!.range.start)
        assertEquals(7, result.range.endInclusive)
        assertEquals(0, result.groups[0]!!.range.start)
        assertEquals(7, result.groups[0]!!.range.endInclusive)
        assertEquals(0, result.groups[1]!!.range.start)
        assertEquals(3, result.groups[1]!!.range.endInclusive)
        assertEquals(4, result.groups[2]!!.range.start)
        assertEquals(7, result.groups[2]!!.range.endInclusive)

        try {
            result.groups[3]
            fail("IndexOutOfBoundsException expected")
        } catch (e: IndexOutOfBoundsException) {
        }

        try {
            result.groupValues[3]
            fail("IndexOutOfBoundsException expected")
        } catch (e: IndexOutOfBoundsException) {
        }

        try {
            result.groups[-1]
            fail("IndexOutOfBoundsException expected")
        } catch (e: IndexOutOfBoundsException) {
        }

        try {
            result.groupValues[-1]
            fail("IndexOutOfBoundsException expected")
        } catch (e: IndexOutOfBoundsException) {
        }

        result = result.next()
        assertTrue(result != null)
        assertEquals(8, result!!.range.start)
        assertEquals(15, result.range.endInclusive)
        assertEquals(8, result.groups[0]!!.range.start)
        assertEquals(15, result.groups[0]!!.range.endInclusive)
        assertEquals(8, result.groups[1]!!.range.start)
        assertEquals(11, result.groups[1]!!.range.endInclusive)
        assertEquals(12, result.groups[2]!!.range.start)
        assertEquals(15, result.groups[2]!!.range.endInclusive)

        try {
            result.groups[3]
            fail("IndexOutOfBoundsException expected")
        } catch (e: IndexOutOfBoundsException) {
        }

        try {
            result.groupValues[3]
            fail("IndexOutOfBoundsException expected")
        } catch (e: IndexOutOfBoundsException) {
        }

        try {
            result.groups[-1]
            fail("IndexOutOfBoundsException expected")
        } catch (e: IndexOutOfBoundsException) {
        }

        try {
            result.groupValues[-1]
            fail("IndexOutOfBoundsException expected")
        } catch (e: IndexOutOfBoundsException) {
        }

        result = result.next()
        assertFalse(result != null)
    }

    /*
 * Regression test for HARMONY-997
 */
    @Test fun testReplacementBackSlash() {
        val str = "replace me"
        val replacedString = "me"
        val substitutionString = "\\"
        val regex = Regex(replacedString)
        try {
            regex.replace(str, substitutionString)
            fail("IllegalArgumentException should be thrown")
        } catch (e: IllegalArgumentException) {
        }
    }

}
