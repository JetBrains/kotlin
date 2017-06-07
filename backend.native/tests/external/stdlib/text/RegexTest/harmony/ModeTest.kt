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

import kotlin.text.*
import kotlin.test.*

/**
 * Tests Pattern compilation modes and modes triggered in pattern strings
 */
fun testCase() {
    var regex: Regex
    var result: MatchResult?

    regex = Regex("([a-z]+)[0-9]+")
    result = regex.find("cAT123#dog345")
    Assert.assertNotNull(result)
    Assert.assertEquals("dog", result!!.groupValues[1])
    Assert.assertNull(result.next())

    regex = Regex("([a-z]+)[0-9]+", RegexOption.IGNORE_CASE)
    result = regex.find("cAt123#doG345")
    Assert.assertNotNull(result)
    Assert.assertEquals("cAt", result!!.groupValues[1])
    result = result.next()
    Assert.assertNotNull(result)
    Assert.assertEquals("doG", result!!.groupValues[1])
    Assert.assertNull(result.next())

    regex = Regex("(?i)([a-z]+)[0-9]+")
    result = regex.find("cAt123#doG345")
    Assert.assertNotNull(result)
    Assert.assertEquals("cAt", result!!.groupValues[1])
    result = result.next()
    Assert.assertNotNull(result)
    Assert.assertEquals("doG", result!!.groupValues[1])
    Assert.assertNull(result.next())
}

fun testMultiline() {
    var regex: Regex
    var result: MatchResult?

    regex = Regex("^foo")
    result = regex.find("foobar")
    Assert.assertNotNull(result)
    Assert.assertTrue(result!!.range.start == 0 && result.range.endInclusive == 2)
    Assert.assertTrue(result.groups[0]!!.range.start == 0 && result.groups[0]!!.range.endInclusive == 2)
    Assert.assertNull(result.next())

    result = regex.find("barfoo")
    Assert.assertNull(result)

    regex = Regex("foo$")
    result = regex.find("foobar")
    Assert.assertNull(result)

    result = regex.find("barfoo")
    Assert.assertNotNull(result)
    Assert.assertTrue(result!!.range.start == 3 && result.range.endInclusive == 5)
    Assert.assertTrue(result.groups[0]!!.range.start == 3 && result.groups[0]!!.range.endInclusive == 5)
    Assert.assertNull(result.next())

    regex = Regex("^foo([0-9]*)", RegexOption.MULTILINE)
    result = regex.find("foo1bar\nfoo2foo3\nbarfoo4")
    Assert.assertNotNull(result)
    Assert.assertEquals("1", result!!.groupValues[1])
    result = result.next()
    Assert.assertNotNull(result)
    Assert.assertEquals("2", result!!.groupValues[1])
    Assert.assertNull(result.next())

    regex = Regex("foo([0-9]*)$", RegexOption.MULTILINE)
    result = regex.find("foo1bar\nfoo2foo3\nbarfoo4")
    Assert.assertNotNull(result)
    Assert.assertEquals("3", result!!.groupValues[1])
    result = result.next()
    Assert.assertNotNull(result)
    Assert.assertEquals("4", result!!.groupValues[1])
    Assert.assertNull(result.next())

    regex = Regex("(?m)^foo([0-9]*)")
    result = regex.find("foo1bar\nfoo2foo3\nbarfoo4")
    Assert.assertNotNull(result)
    Assert.assertEquals("1", result!!.groupValues[1])
    result = result.next()
    Assert.assertNotNull(result)
    Assert.assertEquals("2", result!!.groupValues[1])
    Assert.assertNull(result.next())

    regex = Regex("(?m)foo([0-9]*)$")
    result = regex.find("foo1bar\nfoo2foo3\nbarfoo4")
    Assert.assertNotNull(result)
    Assert.assertEquals("3", result!!.groupValues[1])
    result = result.next()
    Assert.assertNotNull(result)
    Assert.assertEquals("4", result!!.groupValues[1])
    Assert.assertNull(result.next())
}

fun box() {
    testCase()
    testMultiline()
}
