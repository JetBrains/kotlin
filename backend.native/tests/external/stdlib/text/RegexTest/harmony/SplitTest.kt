/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import kotlin.text.*
import kotlin.test.*

fun testSimple() {
    val p = Regex("/")
    val results = p.split("have/you/done/it/right")
    val expected = arrayOf("have", "you", "done", "it", "right")
    Assert.assertEquals(expected.size, results.size)
    for (i in expected.indices) {
        Assert.assertEquals(results[i], expected[i])
    }
}

@Throws(PatternSyntaxException::class)
fun testSplit1() {
    var p = Regex(" ")

    val input = "poodle zoo"
    var tokens: List<String>

    tokens = p.split(input, 1)
    Assert.assertEquals(1, tokens.size)
    Assert.assertTrue(tokens[0] == input)
    tokens = p.split(input, 2)
    Assert.assertEquals(2, tokens.size)
    Assert.assertEquals("poodle", tokens[0])
    Assert.assertEquals("zoo", tokens[1])
    tokens = p.split(input, 5)
    Assert.assertEquals(2, tokens.size)
    Assert.assertEquals("poodle", tokens[0])
    Assert.assertEquals("zoo", tokens[1])
    tokens = p.split(input, 0)
    Assert.assertEquals(2, tokens.size)
    Assert.assertEquals("poodle", tokens[0])
    Assert.assertEquals("zoo", tokens[1])
    tokens = p.split(input)
    Assert.assertEquals(2, tokens.size)
    Assert.assertEquals("poodle", tokens[0])
    Assert.assertEquals("zoo", tokens[1])

    p = Regex("d")

    tokens = p.split(input, 1)
    Assert.assertEquals(1, tokens.size)
    Assert.assertTrue(tokens[0] == input)
    tokens = p.split(input, 2)
    Assert.assertEquals(2, tokens.size)
    Assert.assertEquals("poo", tokens[0])
    Assert.assertEquals("le zoo", tokens[1])
    tokens = p.split(input, 5)
    Assert.assertEquals(2, tokens.size)
    Assert.assertEquals("poo", tokens[0])
    Assert.assertEquals("le zoo", tokens[1])
    tokens = p.split(input, 0)
    Assert.assertEquals(2, tokens.size)
    Assert.assertEquals("poo", tokens[0])
    Assert.assertEquals("le zoo", tokens[1])
    tokens = p.split(input)
    Assert.assertEquals(2, tokens.size)
    Assert.assertEquals("poo", tokens[0])
    Assert.assertEquals("le zoo", tokens[1])

    p = Regex("o")

    tokens = p.split(input, 1)
    Assert.assertEquals(1, tokens.size)
    Assert.assertTrue(tokens[0] == input)
    tokens = p.split(input, 2)
    Assert.assertEquals(2, tokens.size)
    Assert.assertEquals("p", tokens[0])
    Assert.assertEquals("odle zoo", tokens[1])
    tokens = p.split(input, 5)
    Assert.assertEquals(5, tokens.size)
    Assert.assertEquals("p", tokens[0])
    Assert.assertTrue(tokens[1] == "")
    Assert.assertEquals("dle z", tokens[2])
    Assert.assertTrue(tokens[3] == "")
    Assert.assertTrue(tokens[4] == "")
    tokens = p.split(input, 0)
    Assert.assertEquals(5, tokens.size)
    Assert.assertEquals("p", tokens[0])
    Assert.assertTrue(tokens[1] == "")
    Assert.assertEquals("dle z", tokens[2])
    Assert.assertTrue(tokens[3] == "")
    Assert.assertTrue(tokens[4] == "")
    tokens = p.split(input)
    Assert.assertEquals(5, tokens.size)
    Assert.assertEquals("p", tokens[0])
    Assert.assertTrue(tokens[1] == "")
    Assert.assertEquals("dle z", tokens[2])
    Assert.assertTrue(tokens[3] == "")
    Assert.assertTrue(tokens[4] == "")
}

fun testSplit2() {
    val p = Regex("")
    var s: List<String>
    s = p.split("a", 0)
    Assert.assertEquals(3, s.size)
    Assert.assertEquals("", s[0])
    Assert.assertEquals("a", s[1])
    Assert.assertEquals("", s[2])

    s = p.split("", 0)
    Assert.assertEquals(1, s.size)
    Assert.assertEquals("", s[0])

    s = p.split("abcd", 0)
    Assert.assertEquals(6, s.size)
    Assert.assertEquals("", s[0])
    Assert.assertEquals("a", s[1])
    Assert.assertEquals("b", s[2])
    Assert.assertEquals("c", s[3])
    Assert.assertEquals("d", s[4])
    Assert.assertEquals("", s[5])
}

fun testSplitSupplementaryWithEmptyString() {

    /*
     * See http://www.unicode.org/reports/tr18/#Supplementary_Characters We
     * have to treat text as code points not code units.
     */
    val p = Regex("")
    val s: List<String>
    s = p.split("a\ud869\uded6b", 0)
    Assert.assertEquals(5, s.size)
    Assert.assertEquals("", s[0])
    Assert.assertEquals("a", s[1])
    Assert.assertEquals("\ud869\uded6", s[2])
    Assert.assertEquals("b", s[3])
    Assert.assertEquals("", s[4])
}

fun box() {
    testSimple()
    testSplit1()
    testSplit2()
    testSplitSupplementaryWithEmptyString()
}
