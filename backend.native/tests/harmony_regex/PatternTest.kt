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

package test.text.harmony_regex

import kotlin.text.*
import kotlin.test.*

class PatternTest {

    fun assertTrue(msg: String, value: Boolean) = assertTrue(value, msg)
    fun assertFalse(msg: String, value: Boolean) = assertFalse(value, msg)

    internal var testPatterns = arrayOf("(a|b)*abb", "(1*2*3*4*)*567", "(a|b|c|d)*aab", "(1|2|3|4|5|6|7|8|9|0)(1|2|3|4|5|6|7|8|9|0)*", "(abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ)*", "(a|b)*(a|b)*A(a|b)*lice.*", "(a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z)(a|b|c|d|e|f|g|h|" + "i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z)*(1|2|3|4|5|6|7|8|9|0)*|while|for|struct|if|do", "x(?c)y", "x(?cc)y", "x(?:c)y")

    @Test fun testCommentsInPattern() {
        val p = Regex("ab# this is a comment\ncd", RegexOption.COMMENTS)
        assertTrue(p.matches("abcd"))
    }

    @Test fun testSplitCharSequenceint() {
        // Splitting CharSequence which ends with pattern.
        // Harmony regress tests.
        assertEquals(",,".split(",".toRegex(), 3).toTypedArray().size, 3)
        assertEquals(",,".split(",".toRegex(), 4).toTypedArray().size, 3)
        assertEquals(Regex("o").split("boo:and:foo", 5).size, 5)
        assertEquals(Regex("b").split("ab", 0).size, 2)
        var s: List<String>
        var regex = Regex("x")
        s = regex.split("zxx:zzz:zxx", 10)
        assertEquals(s.size, 5)
        s = regex.split("zxx:zzz:zxx", 3)
        assertEquals(s.size, 3)
        s = regex.split("zxx:zzz:zxx", 0)
        assertEquals(s.size, 5)

        // Other splitting.
        // Negative limit
        regex = Regex("b")
        s = regex.split("abccbadfebb", 0)
        assertEquals(s.size, 5)
        s = regex.split("", 0)
        assertEquals(s.size, 1)
        regex = Regex("")
        s = regex.split("", 0)
        assertEquals(s.size, 2)
        s = regex.split("abccbadfe", 0)
        assertEquals(s.size, 11)

        // positive limit
        regex = Regex("b")
        s = regex.split("abccbadfebb", 12)
        assertEquals(s.size, 5)
        s = regex.split("", 6)
        assertEquals(s.size, 1)
        regex = Regex("")
        s = regex.split("", 11)
        assertEquals(s.size, 2)
        s = regex.split("abccbadfe", 15)
        assertEquals(s.size, 11)

        regex = Regex("b")
        s = regex.split("abccbadfebb", 5)
        assertEquals(s.size, 5)
        s = regex.split("", 1)
        assertEquals(s.size, 1)
        regex = Regex("")
        s = regex.split("", 1)
        assertEquals(s.size, 1)
        s = regex.split("abccbadfe", 11)
        assertEquals(s.size, 11)

        regex = Regex("b")
        s = regex.split("abccbadfebb", 3)
        assertEquals(s.size, 3)
        regex = Regex("")
        s = regex.split("abccbadfe", 5)
        assertEquals(s.size, 5)
    }

    @Test fun testFlags() {
        var baseString: String
        var testString: String
        var regex: Regex

        baseString = "((?i)|b)a"
        testString = "A"
        regex = Regex(baseString)
        assertFalse(regex.matches(testString))

        baseString = "(?i)a|b"
        testString = "A"
        regex = Regex(baseString)
        assertTrue(regex.matches(testString))

        baseString = "(?i)a|b"
        testString = "B"
        regex = Regex(baseString)
        assertTrue(regex.matches(testString))

        baseString = "c|(?i)a|b"
        testString = "B"
        regex = Regex(baseString)
        assertTrue(regex.matches(testString))

        baseString = "(?i)a|(?s)b"
        testString = "B"
        regex = Regex(baseString)
        assertTrue(regex.matches(testString))

        baseString = "(?i)a|(?-i)b"
        testString = "B"
        regex = Regex(baseString)
        assertFalse(regex.matches(testString))

        baseString = "(?i)a|(?-i)c|b"
        testString = "B"
        regex = Regex(baseString)
        assertFalse(regex.matches(testString))

        baseString = "(?i)a|(?-i)c|(?i)b"
        testString = "B"
        regex = Regex(baseString)
        assertTrue(regex.matches(testString))

        baseString = "(?i)a|(?-i)b"
        testString = "A"
        regex = Regex(baseString)
        assertTrue(regex.matches(testString))

        baseString = "((?i))a"
        testString = "A"
        regex = Regex(baseString)
        assertFalse(regex.matches(testString))

        baseString = "|(?i)|a"
        testString = "A"
        regex = Regex(baseString)
        assertTrue(regex.matches(testString))

        baseString = "(?i)((?s)a.)"
        testString = "A\n"
        regex = Regex(baseString)
        assertTrue(regex.matches(testString))

        baseString = "(?i)((?-i)a)"
        testString = "A"
        regex = Regex(baseString)
        assertFalse(regex.matches(testString))

        baseString = "(?i)(?s:a.)"
        testString = "A\n"
        regex = Regex(baseString)
        assertTrue(regex.matches(testString))

        baseString = "(?i)fgh(?s:aa)"
        testString = "fghAA"
        regex = Regex(baseString)
        assertTrue(regex.matches(testString))

        baseString = "(?i)((?-i))a"
        testString = "A"
        regex = Regex(baseString)
        assertTrue(regex.matches(testString))

        baseString = "abc(?i)d"
        testString = "ABCD"
        regex = Regex(baseString)
        assertFalse(regex.matches(testString))

        testString = "abcD"
        assertTrue(regex.matches(testString))

        baseString = "a(?i)a(?-i)a(?i)a(?-i)a"
        testString = "aAaAa"
        regex = Regex(baseString)
        assertTrue(regex.matches(testString))

        testString = "aAAAa"
        assertFalse(regex.matches(testString))
    }

    fun Set<RegexOption>.containsOnly(vararg options: RegexOption): Boolean {
        val toCheck = options.toSet()
        return size == toCheck.size && containsAll(toCheck)
    }

    @Test fun testFlagsMethod() {
        val a = kotlin.text.Regex("sdf")
        var baseString: String
        var regex: Regex

        baseString = "(?-i)"
        regex = Regex(baseString)

        baseString = "(?idmsux)abc(?-i)vg(?-dmu)"
        regex = Regex(baseString)
        assertTrue(regex.options.containsOnly(RegexOption.DOT_MATCHES_ALL, RegexOption.COMMENTS))

        baseString = "(?idmsux)abc|(?-i)vg|(?-dmu)"
        regex = Regex(baseString)
        assertTrue(regex.options.containsOnly(RegexOption.DOT_MATCHES_ALL, RegexOption.COMMENTS))

        baseString = "(?is)a((?x)b.)"
        regex = Regex(baseString)
        assertTrue(regex.options.containsOnly(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))

        baseString = "(?i)a((?-i))"
        regex = Regex(baseString)
        assertTrue(regex.options.containsOnly(RegexOption.IGNORE_CASE))

        baseString = "((?i)a)"
        regex = Regex(baseString)
        assertTrue(regex.options.isEmpty())

        regex = Regex("(?is)abc")
        assertTrue(regex.options.containsOnly(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    }

    @Test fun testCompileStringint() {
        /*
         * this tests are needed to verify that appropriate exceptions are hrown
         */
        var pattern = "b)a"
        try {
            Regex(pattern)
            fail("Expected a IllegalArgumentException when compiling pattern: " + pattern)
        } catch (e: IllegalArgumentException) {
            // pass
        }

        pattern = "bcde)a"
        try {
            Regex(pattern)
            fail("Expected a IllegalArgumentException when compiling pattern: " + pattern)
        } catch (e: IllegalArgumentException) {
            // pass
        }

        pattern = "bbg())a"
        try {
            Regex(pattern)
            fail("Expected a IllegalArgumentException when compiling pattern: " + pattern)
        } catch (e: IllegalArgumentException) {
            // pass
        }

        pattern = "cdb(?i))a"
        try {
            Regex(pattern)
            fail("Expected a IllegalArgumentException when compiling pattern: " + pattern)
        } catch (e: IllegalArgumentException) {
            // pass
        }

        /*
         * This pattern should compile (Originally it is a regression test for HARMONY-2127)
         */
        pattern = "x(?c)y"
        Regex(pattern)

        /*
         * This pattern doesn't match any string, but should be compiled anyway
         */
        pattern = "(b\\u0001)a"
        Regex(pattern)
    }


    @Test fun testQuantCompileNeg() {
        val patterns = arrayOf("5{,2}", "{5asd", "{hgdhg", "{5,hjkh", "{,5hdsh", "{5,3shdfkjh}")
        for (element in patterns) {
            try {
                Regex(element)
                fail("IllegalArgumentException was expected, but compilation succeeds")
            } catch (pse: IllegalArgumentException) {
                continue
            }

        }
    }

    @Test fun testQuantCompilePos() {
        val patterns = arrayOf("abc{2,}", "abc{5}")
        for (element in patterns) {
            Regex(element)
        }
    }

    @Test fun testQuantComposition() {
        val pattern = "(a{1,3})aab"
        val regex = Regex(pattern)
        val result = regex.matchEntire("aaab")
        assertNotNull(result)
        assertEquals(result!!.groups[1]!!.range.start, 0)
        assertEquals(result.groupValues[1], "a")
    }

    @Test fun testTimeZoneIssue() {
        val regex = Regex("GMT(\\+|\\-)(\\d+)(:(\\d+))?")
        val result = regex.matchEntire("GMT-9:45")
        assertNotNull(result)
        assertEquals("-", result!!.groupValues[1])
        assertEquals("9", result.groupValues[2])
        assertEquals(":45", result.groupValues[3])
        assertEquals("45", result.groupValues[4])
    }

    @Test fun testCompileRanges() {
        val correctTestPatterns = arrayOf("[^]*abb]*", "[^a-d[^m-p]]*abb", "[a-d\\d]*abb", "[abc]*abb",
                "[a-e&&[de]]*abb", "[^abc]*abb", "[a-e&&[^de]]*abb", "[a-z&&[^m-p]]*abb", "[a-d[m-p]]*abb",
                "[a-zA-Z]*abb", "[+*?]*abb", "[^+*?]*abb")

        val inputSecuence = arrayOf("kkkk", "admpabb", "abcabcd124654abb", "abcabccbacababb",
                "dededededededeedabb", "gfdhfghgdfghabb", "accabacbcbaabb", "acbvfgtyabb", "adbcacdbmopabcoabb",
                "jhfkjhaSDFGHJkdfhHNJMjkhfabb", "+*??+*abb", "sdfghjkabb")

        for (i in correctTestPatterns.indices) {
            assertTrue("pattern: " + correctTestPatterns[i] + " input: " + inputSecuence[i],
                    Regex(correctTestPatterns[i]).matches(inputSecuence[i]))
        }

        val wrongInputSecuence = arrayOf("]", "admpkk", "abcabcd124k654abb", "abwcabccbacababb",
                "abababdeababdeabb", "abcabcacbacbabb", "acdcbecbaabb", "acbotyabb", "adbcaecdbmopabcoabb",
                "jhfkjhaSDFGHJk;dfhHNJMjkhfabb", "+*?a?+*abb", "sdf+ghjkabb")

        for (i in correctTestPatterns.indices) {
            assertFalse("pattern: " + correctTestPatterns[i] + " input: " + wrongInputSecuence[i],
                    Regex(correctTestPatterns[i]).matches(wrongInputSecuence[i]))
        }
    }

    @Test fun testRangesSpecialCases() {
        val neg_patterns = arrayOf("[a-&&[b-c]]", "[a-\\w]", "[b-a]", "[]")

        for (element in neg_patterns) {
            try {
                Regex(element)
                fail("IllegalArgumentException was expected: " + element)
            } catch (pse: IllegalArgumentException) {
            }

        }

        val pos_patterns = arrayOf("[-]+", "----", "[a-]+", "a-a-a-a-aa--", "[\\w-a]+", "123-2312--aaa-213", "[a-]]+", "-]]]]]]]]]]]]]]]")

        var i = 0
        while (i < pos_patterns.size) {
            val pat = pos_patterns[i++]
            val inp = pos_patterns[i]
            assertTrue("pattern: $pat input: $inp", Regex(pat).matches(inp))
            i++
        }
    }

    @Test fun testZeroSymbols() {
        assertTrue(Regex("[\u0000]*abb").matches("\u0000\u0000\u0000\u0000\u0000\u0000abb"))
    }

    @Test fun testEscapes() {
        val regex = Regex("\\Q{]()*?")
        assertTrue(regex.matches("{]()*?"))
    }

    @Test fun testRegressions() {
        // Bug 181
        Regex("[\\t-\\r]")

        // HARMONY-4472
        Regex("a*.+")

        // Bug187
        Regex("|(?idmsux-idmsux)|(?idmsux-idmsux)|[^|\\[-\\0274|\\,-\\\\[^|W\\}\\nq\\x65\\002\\xFE\\05\\06\\00\\x66\\x47i\\,\\xF2\\=\\06\\u0EA4\\x9B\\x3C\\f\\|\\{\\xE5\\05\\r\\u944A\\xCA\\e|\\x19\\04\\x07\\04\\u607B\\023\\0073\\x91Tr\\0150\\x83]]?(?idmsux-idmsux:\\p{Alpha}{7}?)||(?<=[^\\uEC47\\01\\02\\u3421\\a\\f\\a\\013q\\035w\\e])(?<=\\p{Punct}{0,}?)(?=^\\p{Lower})(?!\\b{8,14})(?<![|\\00-\\0146[^|\\04\\01\\04\\060\\f\\u224DO\\x1A\\xC4\\00\\02\\0315\\0351\\u84A8\\xCBt\\xCC\\06|\\0141\\00\\=\\e\\f\\x6B\\0026Tb\\040\\x76xJ&&[\\\\-\\]\\05\\07\\02\\u2DAF\\t\\x9C\\e\\0023\\02\\,X\\e|\\u6058flY\\u954C]]]{5}?)(?<=\\p{Sc}{8}+)[^|\\026-\\u89BA|o\\u6277\\t\\07\\x50&&\\p{Punct}]{8,14}+((?<=^\\p{Punct})|(?idmsux-idmsux)||(?>[\\x3E-\\]])|(?idmsux-idmsux:\\p{Punct})|(?<![\\0111\\0371\\xDF\\u6A49\\07\\u2A4D\\00\\0212\\02Xd-\\xED[^\\a-\\0061|\\0257\\04\\f\\[\\0266\\043\\03\\x2D\\042&&[^\\f-\\]&&\\s]]])|(?>[|\\n\\042\\uB09F\\06\\u0F2B\\uC96D\\x89\\uC166\\xAA|\\04-\\][^|\\a\\|\\rx\\04\\uA770\\n\\02\\t\\052\\056\\0274\\|\\=\\07\\e|\\00-\\x1D&&[^\\005\\uB15B\\uCDAC\\n\\x74\\0103\\0147\\uD91B\\n\\062G\\u9B4B\\077\\}\\0324&&[^\\0302\\,\\0221\\04\\u6D16\\04xy\\uD193\\[\\061\\06\\045\\x0F|\\e\\xBB\\f\\u1B52\\023\\u3AD2\\033\\007\\022\\}\\x66\\uA63FJ-\\0304]]]]{0,0})||(?<![^|\\0154U\\u0877\\03\\fy\\n\\|\\0147\\07-\\=[|q\\u69BE\\0243\\rp\\053\\02\\x33I\\u5E39\\u9C40\\052-\\xBC[|\\0064-\\?|\\uFC0C\\x30\\0060\\x45\\\\\\02\\?p\\xD8\\0155\\07\\0367\\04\\uF07B\\000J[^|\\0051-\\{|\\u9E4E\\u7328\\]\\u6AB8\\06\\x71\\a\\]\\e\\|KN\\u06AA\\0000\\063\\u2523&&[\\005\\0277\\x41U\\034\\}R\\u14C7\\u4767\\x09\\n\\054Ev\\0144\\<\\f\\,Q-\\xE4]]]]]{3}+)|(?>^+)|(?![^|\\|\\nJ\\t\\<\\04E\\\\\\t\\01\\\\\\02\\|\\=\\}\\xF3\\uBEC2\\032K\\014\\uCC5F\\072q\\|\\0153\\xD9\\0322\\uC6C8[^\\t\\0342\\x34\\x91\\06\\{\\xF1\\a\\u1710\\?\\xE7\\uC106\\02pF\\<&&[^|\\]\\064\\u381D\\u50CF\\eO&&[^|\\06\\x2F\\04\\045\\032\\u8536W\\0377\\0017|\\x06\\uE5FA\\05\\xD4\\020\\04c\\xFC\\02H\\x0A\\r]]]]+?)(?idmsux-idmsux)|(?<![|\\r-\\,&&[I\\t\\r\\0201\\xDB\\e&&[^|\\02\\06\\00\\<\\a\\u7952\\064\\051\\073\\x41\\?n\\040\\0053\\031&&[\\x15-\\|]]]]{8,11}?)(?![^|\\<-\\uA74B\\xFA\\u7CD2\\024\\07n\\<\\x6A\\0042\\uE4FF\\r\\u896B\\[\\=\\042Y&&^\\p{ASCII}]++)|(?<![R-\\|&&[\\a\\0120A\\u6145\\<\\050-d[|\\e-\\uA07C|\\016-\\u80D9]]]{1,}+)|(?idmsux-idmsux)|(?idmsux-idmsux)|(?idmsux-idmsux:\\B{6,}?)|(?<=\\D{5,8}?)|(?>[\\{-\\0207|\\06-\\0276\\p{XDigit}])(?idmsux-idmsux:[^|\\x52\\0012\\]u\\xAD\\0051f\\0142\\\\l\\|\\050\\05\\f\\t\\u7B91\\r\\u7763\\{|h\\0104\\a\\f\\0234\\u2D4F&&^\\P{InGreek}]))")

        // HARMONY-5858
        Regex("\\u6211", RegexOption.LITERAL)
    }

    @Test fun testOrphanQuantifiers() {
        try {
            Regex("+++++")
            fail("IllegalArgumentException expected")
        } catch (pse: IllegalArgumentException) {
        }

    }

    @Test fun testOrphanQuantifiers2() {
        try {
            Regex("\\d+*")
            fail("IllegalArgumentException expected")
        } catch (pse: IllegalArgumentException) {
        }

    }

    @Test fun testBug197() {
        val vals = arrayOf<Any>(":", 2, arrayOf("boo", "and:foo"),
                ":", 5, arrayOf("boo", "and", "foo"),
                ":", 0, arrayOf("boo", "and", "foo"),
                ":", 3, arrayOf("boo", "and", "foo"),
                ":", 1, arrayOf("boo:and:foo"),
                "o", 5, arrayOf("b", "", ":and:f", "", ""),
                "o", 4, arrayOf("b", "", ":and:f", "o"),
                "o", 0, arrayOf("b", "", ":and:f", "", "")
        )

        var i = 0
        while (i < vals.size / 3) {
            val res = Regex(vals[i++].toString()).split("boo:and:foo", (vals[i++] as Int))
            val expectedRes = vals[i++] as Array<String>

            assertEquals(expectedRes.size, res.size)

            for (j in expectedRes.indices) {
                assertEquals(expectedRes[j], res[j])
            }
        }
    }

    @Test fun testURIPatterns() {
        val URI_REGEXP_STR = "^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?";
        val SCHEME_REGEXP_STR = "^[a-zA-Z]{1}[\\w+-.]+$";
        val REL_URI_REGEXP_STR = "^(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?";
        val IPV6_REGEXP_STR = "^[0-9a-fA-F\\:\\.]+(\\%\\w+)?$";
        val IPV6_REGEXP_STR2 = "^\\[[0-9a-fA-F\\:\\.]+(\\%\\w+)?\\]$";
        val IPV4_REGEXP_STR = "^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}$";
        val HOSTNAME_REGEXP_STR = "\\w+[\\w\\-\\.]*";

        Regex(URI_REGEXP_STR)
        Regex(REL_URI_REGEXP_STR)
        Regex(SCHEME_REGEXP_STR)
        Regex(IPV4_REGEXP_STR)
        Regex(IPV6_REGEXP_STR)
        Regex(IPV6_REGEXP_STR2)
        Regex(HOSTNAME_REGEXP_STR)
    }

    @Test fun testFindBoundaryCases1() {
        val regex = Regex(".*\n")
        val result = regex.find("a\n")

        assertNotNull(result)
        assertEquals("a\n", result!!.value)
    }

    @Test fun testFindBoundaryCases2() {
        val regex = Regex(".*A")
        val result = regex.find("aAa")

        assertNotNull(result)
        assertEquals("aA", result!!.value)

    }

    @Test fun testFindBoundaryCases3() {
        val regex = Regex(".*A")
        val result = regex.find("a\naA\n")

        assertNotNull(result)
        assertEquals("aA", result!!.value)

    }

    @Test fun testFindBoundaryCases4() {
        val regex = Regex("A.*")
        val result = regex.find("A\n")

        assertNotNull(result)
        assertEquals("A", result!!.value)

    }

    @Test fun testFindBoundaryCases5() {
        val regex = Regex(".*A.*")
        var result = regex.find("\nA\naaa\nA\naaAaa\naaaA\n")
        val expected = arrayOf("A", "A", "aaAaa", "aaaA")

        var k = 0
        while (result != null) {
            assertEquals(expected[k], result.value)
            result = result.next()
            k++
        }
    }

    @Test fun testFindBoundaryCases6() {
        val regex = Regex(".*")
        var result = regex.find("\na\n")
        val expected = arrayOf("", "a", "", "")

        var k = 0
        while (result != null) {
            assertEquals(expected[k], result.value)
            k++
            result = result.next()
        }
    }

    @Test fun testBackReferences() {
        var regex = Regex("(\\((\\w*):(.*):(\\2)\\))")
        var result = regex.find("(start1: word :start1)(start2: word :start2)")

        var k = 1
        while (result != null) {
            assertEquals("start" + k, result.groupValues[2])
            assertEquals(" word ", result.groupValues[3])
            assertEquals("start" + k, result.groupValues[4])
            k++
            result = result.next()
        }

        assertEquals(3, k)
        regex = Regex(".*(.)\\1")
        assertTrue(regex.matches("saa"))
    }

    @Test fun testNewLine() {
        val regex = Regex("(^$)*\n", RegexOption.MULTILINE)
        var result = regex.find("\r\n\n")
        var counter = 0
        while (result != null) {
            counter++
            result = result.next()
        }
        assertEquals(2, counter)
    }

    @Test fun testFindGreedy() {
        val regex = Regex(".*aaa", RegexOption.DOT_MATCHES_ALL)
        val result = regex.matchEntire("aaaa\naaa\naaaaaa")
        assertNotNull(result)
        assertEquals(14, result!!.range.endInclusive)
    }

    @Test fun testSOLQuant() {
        val regex = Regex("$*", RegexOption.MULTILINE)
        var result = regex.find("\n\n")
        var counter = 0
        while (result != null) {
            counter++
            result = result.next()
        }
        assertEquals(3, counter)
    }

    @Test fun testIllegalEscape() {
        try {
            Regex("\\y")
            fail("IllegalArgumentException expected")
        } catch (pse: IllegalArgumentException) {
        }
    }

    @Test fun testEmptyFamily() {
        Regex("\\p{Lower}")
    }

    @Test fun testNonCaptConstr() {
        // Flags
        var regex = Regex("(?i)b*(?-i)a*")
        assertTrue(regex.matches("bBbBaaaa"))
        assertFalse(regex.matches("bBbBAaAa"))

        // Non-capturing groups
        regex = Regex("(?i:b*)a*")
        assertTrue(regex.matches("bBbBaaaa"))
        assertFalse(regex.matches("bBbBAaAa"))

        // 1 2 3 4 5 6 7 8 9 10 11
        regex = Regex("(?:-|(-?\\d+\\d\\d\\d))?(?:-|-(\\d\\d))?(?:-|-(\\d\\d))?(T)?(?:(\\d\\d):(\\d\\d):(\\d\\d)(\\.\\d+)?)?(?:(?:((?:\\+|\\-)\\d\\d):(\\d\\d))|(Z))?")
        val result = regex.matchEntire("-1234-21-31T41:51:61.789+71:81")
        assertNotNull(result)
        assertEquals("-1234", result!!.groupValues[1])
        assertEquals("21", result.groupValues[2])
        assertEquals("31", result.groupValues[3])
        assertEquals("T", result.groupValues[4])
        assertEquals("41", result.groupValues[5])
        assertEquals("51", result.groupValues[6])
        assertEquals("61", result.groupValues[7])
        assertEquals(".789", result.groupValues[8])
        assertEquals("+71", result.groupValues[9])
        assertEquals("81", result.groupValues[10])

        // positive lookahead
        regex = Regex(".*\\.(?=log$).*$")
        assertTrue(regex.matches("a.b.c.log"))
        assertFalse(regex.matches("a.b.c.log."))

        // negative lookahead
        regex = Regex(".*\\.(?!log$).*$")
        assertFalse(regex.matches("abc.log"))
        assertTrue(regex.matches("abc.logg"))

        // positive lookbehind
        regex = Regex(".*(?<=abc)\\.log$")
        assertFalse(regex.matches("cde.log"))
        assertTrue(regex.matches("abc.log"))

        // negative lookbehind
        regex = Regex(".*(?<!abc)\\.log$")
        assertTrue(regex.matches("cde.log"))
        assertFalse(regex.matches("abc.log"))

        // atomic group
        regex = Regex("(?>a*)abb")
        assertFalse(regex.matches("aaabb"))
        regex = Regex("(?>a*)bb")
        assertTrue(regex.matches("aaabb"))

        regex = Regex("(?>a|aa)aabb")
        assertTrue(regex.matches("aaabb"))
        regex = Regex("(?>aa|a)aabb")
        assertFalse(regex.matches("aaabb"))

        // quantifiers over look ahead
        regex = Regex(".*(?<=abc)*\\.log$")
        assertTrue(regex.matches("cde.log"))
        regex = Regex(".*(?<=abc)+\\.log$")
        assertFalse(regex.matches("cde.log"))
    }

    @Test fun testCompilePatternWithTerminatorMark() {
        val regex = Regex("a\u0000\u0000cd")
        assertTrue(regex.matches("a\u0000\u0000cd"))
    }

    @Test fun testAlternations() {
        var baseString = "|a|bc"
        var regex = Regex(baseString)
        assertTrue(regex.matches(""))

        baseString = "a||bc"
        regex = Regex(baseString)
        assertTrue(regex.matches(""))

        baseString = "a|bc|"
        regex = Regex(baseString)
        assertTrue(regex.matches(""))

        baseString = "a|b|"
        regex = Regex(baseString)
        assertTrue(regex.matches(""))

        baseString = "a(|b|cd)e"
        regex = Regex(baseString)
        assertTrue(regex.matches("ae"))

        baseString = "a(b||cd)e"
        regex = Regex(baseString)
        assertTrue(regex.matches("ae"))

        baseString = "a(b|cd|)e"
        regex = Regex(baseString)
        assertTrue(regex.matches("ae"))

        baseString = "a(b|c|)e"
        regex = Regex(baseString)
        assertTrue(regex.matches("ae"))

        baseString = "a(|)e"
        regex = Regex(baseString)
        assertTrue(regex.matches("ae"))

        baseString = "|"
        regex = Regex(baseString)
        assertTrue(regex.matches(""))

        baseString = "a(?:|)e"
        regex = Regex(baseString)
        assertTrue(regex.matches("ae"))

        baseString = "a||||bc"
        regex = Regex(baseString)
        assertTrue(regex.matches(""))

        baseString = "(?i-is)|a"
        regex = Regex(baseString)
        assertTrue(regex.matches("a"))
    }

    @Test fun testMatchWithGroups() {
        var baseString = "jwkerhjwehrkwjehrkwjhrwkjehrjwkehrjkwhrkwehrkwhrkwrhwkhrwkjehr"
        var pattern = ".*(..).*\\1.*"
        assertTrue(Regex(pattern).matches(baseString))

        baseString = "saa"
        pattern = ".*(.)\\1"
        assertTrue(Regex(pattern).matches(baseString))
        assertTrue(Regex(pattern).containsMatchIn(baseString))
    }

    @Test fun testSplitEmptyCharSequence() {
        val s1 = ""
        val arr = s1.split(":".toRegex())
        assertEquals(arr.size, 1)
    }

    @Test fun testSplitEndsWithPattern() {
        assertEquals(",,".split(",".toRegex(), 3).toTypedArray().size, 3)
        assertEquals(",,".split(",".toRegex(), 4).toTypedArray().size, 3)

        assertEquals(Regex("o").split("boo:and:foo", 5).size, 5)
        assertEquals(Regex("b").split("ab", 0).size, 2)
    }

    @Test fun testCaseInsensitiveFlag() {
        assertTrue(Regex("(?i-:AbC)").matches("ABC"))
    }

    @Test fun testEmptyGroups() {
        var regex = Regex("ab(?>)cda")
        assertTrue(regex.matches("abcda"))

        regex = Regex("ab()")
        assertTrue(regex.matches("ab"))

        regex = Regex("abc(?:)(..)")
        assertTrue(regex.matches("abcgf"))
    }

    @Test fun testCompileNonCaptGroup() {
        var isCompiled = false

        try {
            Regex("(?:)", RegexOption.CANON_EQ)
            Regex("(?:)", setOf(RegexOption.CANON_EQ, RegexOption.DOT_MATCHES_ALL))
            Regex("(?:)", setOf(RegexOption.CANON_EQ, RegexOption.IGNORE_CASE))
            Regex("(?:)", setOf(RegexOption.CANON_EQ, RegexOption.COMMENTS, RegexOption.UNIX_LINES))
            isCompiled = true
        } catch (e: IllegalArgumentException) {
            println(e)
        }
        assertTrue(isCompiled)
    }

    @Test fun testEmbeddedFlags() {
        var baseString = "(?i)((?s)a)"
        var testString = "A"
        var regex = Regex(baseString)
        assertTrue(regex.matches(testString))

        baseString = "(?x)(?i)(?s)(?d)a"
        testString = "A"
        regex = Regex(baseString)
        assertTrue(regex.matches(testString))

        baseString = "(?x)(?i)(?s)(?d)a."
        testString = "a\n"
        regex = Regex(baseString)
        assertTrue(regex.matches(testString))

        baseString = "abc(?x:(?i)(?s)(?d)a.)"
        testString = "abcA\n"
        regex = Regex(baseString)
        assertTrue(regex.matches(testString))

        baseString = "abc((?x)d)(?i)(?s)a"
        testString = "abcdA"
        regex = Regex(baseString)
        assertTrue(regex.matches(testString))
    }

    @Test fun testAltWithFlags() {
        Regex("|(?i-xi)|()")
    }

    @Test fun testRestoreFlagsAfterGroup() {
        val baseString = "abc((?x)d)   a"
        val testString = "abcd   a"
        val regex = Regex(baseString)
        assertTrue(regex.matches(testString))
    }

    @Test fun testCanonEqFlag() {

        /*
         * for decompositions see
         * http://www.unicode.org/Public/4.0-Update/UnicodeData-4.0.0.txt
         * http://www.unicode.org/reports/tr15/#Decomposition
         */
        var baseString: String
        var testString: String
        var regex: Regex

        baseString = "ab(a*)\\u0001"
        regex = Regex(baseString, RegexOption.CANON_EQ)

        baseString = "a(abcdf)d"
        regex = Regex(baseString, RegexOption.CANON_EQ)

        baseString = "aabcdfd"
        regex = Regex(baseString, RegexOption.CANON_EQ)

        // \u01E0 -> \u0226\u0304 ->\u0041\u0307\u0304
        // \u00CC -> \u0049\u0300

        baseString = "\u01E0\u00CCcdb(ac)"
        testString = "\u0226\u0304\u0049\u0300cdbac"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))

        baseString = "\u01E0cdb(a\u00CCc)"
        testString = "\u0041\u0307\u0304cdba\u0049\u0300c"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))

        baseString = "a\u00CC"
        testString = "a\u0049\u0300"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))

        baseString = "\u0226\u0304cdb(ac\u0049\u0300)"
        testString = "\u01E0cdbac\u00CC"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))

        baseString = "cdb(?:\u0041\u0307\u0304\u00CC)"
        testString = "cdb\u0226\u0304\u0049\u0300"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))

        baseString = "\u01E0[a-c]\u0049\u0300cdb(ac)"
        testString = "\u01E0b\u00CCcdbac"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))

        baseString = "\u01E0|\u00CCcdb(ac)"
        testString = "\u0041\u0307\u0304"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))

        baseString = "\u00CC?cdb(ac)*(\u01E0)*[a-c]"
        testString = "cdb\u0041\u0307\u0304b"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))

        baseString = "a\u0300"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertTrue(regex.containsMatchIn("a\u00E0a"))

        baseString = "\u7B20\uF9F8abc"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertTrue(regex.matches("\uF9F8\uF9F8abc"))

        // \u01F9 -> \u006E\u0300
        // \u00C3 -> \u0041\u0303

        baseString = "cdb(?:\u00C3\u006E\u0300)"
        testString = "cdb\u0041\u0303\u01F9"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))

        // \u014C -> \u004F\u0304
        // \u0163 -> \u0074\u0327

        baseString = "cdb(?:\u0163\u004F\u0304)"
        testString = "cdb\u0074\u0327\u014C"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))

        // \u00E1->a\u0301
        // canonical ordering takes place \u0301\u0327 -> \u0327\u0301

        baseString = "c\u0327\u0301"
        testString = "c\u0301\u0327"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))

        /*
         * Hangul decompositions
         */
        // \uD4DB->\u1111\u1171\u11B6
        // \uD21E->\u1110\u116D\u11B5
        // \uD264->\u1110\u1170
        // not Hangul:\u0453->\u0433\u0301
        baseString = "a\uD4DB\u1111\u1171\u11B6\uD264"
        regex = Regex(baseString, RegexOption.CANON_EQ)

        baseString = "\u0453c\uD4DB"
        regex = Regex(baseString, RegexOption.CANON_EQ)

        baseString = "a\u1110\u116D\u11B5b\uD21Ebc"
        regex = Regex(baseString, RegexOption.CANON_EQ)

        baseString = "\uD4DB\uD21E\u1110\u1170cdb(ac)"
        testString = "\u1111\u1171\u11B6\u1110\u116D\u11B5\uD264cdbac"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))

        baseString = "\uD4DB\uD264cdb(a\uD21Ec)"
        testString = "\u1111\u1171\u11B6\u1110\u1170cdba\u1110\u116D\u11B5c"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))

        baseString = "a\uD4DB"
        testString = "a\u1111\u1171\u11B6"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))

        baseString = "a\uD21E"
        testString = "a\u1110\u116D\u11B5"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))

        baseString = "\u1111\u1171\u11B6cdb(ac\u1110\u116D\u11B5)"
        testString = "\uD4DBcdbac\uD21E"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))

        baseString = "cdb(?:\u1111\u1171\u11B6\uD21E)"
        testString = "cdb\uD4DB\u1110\u116D\u11B5"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))

        baseString = "\uD4DB[a-c]\u1110\u116D\u11B5cdb(ac)"
        testString = "\uD4DBb\uD21Ecdbac"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))

        baseString = "\uD4DB|\u00CCcdb(ac)"
        testString = "\u1111\u1171\u11B6"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))

        baseString = "\uD4DB|\u00CCcdb(ac)"
        testString = "\u1111\u1171"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertFalse(regex.matches(testString))

        baseString = "\u00CC?cdb(ac)*(\uD4DB)*[a-c]"
        testString = "cdb\u1111\u1171\u11B6b"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))

        baseString = "\uD4DB"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertTrue(regex.containsMatchIn("a\u1111\u1171\u11B6a"))

        baseString = "\u1111"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        assertFalse(regex.containsMatchIn("bcda\uD4DBr"))
    }

    @Test fun testIndexesCanonicalEq() {
        var baseString: String
        var testString: String
        var regex: Regex
        var result: MatchResult?

        baseString = "\uD4DB"
        testString = "bcda\u1111\u1171\u11B6awr"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        result = regex.find(testString)
        assertNotNull(result)
        assertEquals(result!!.range.start, 4)
        assertEquals(result.range.endInclusive, 6)

        baseString = "\uD4DB\u1111\u1171\u11B6"
        testString = "bcda\u1111\u1171\u11B6\uD4DBawr"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        result = regex.find(testString) // Use the same testString
        assertNotNull(result)
        assertEquals(result!!.range.start, 4)
        assertEquals(result.range.endInclusive, 7)

        baseString = "\uD4DB\uD21E\u1110\u1170"
        testString = "abcabc\u1111\u1171\u11B6\u1110\u116D\u11B5\uD264cdbac"
        regex = Regex(baseString, RegexOption.CANON_EQ)
        result = regex.find(testString)
        assertNotNull(result)
        assertEquals(result!!.range.start, 6)
        assertEquals(result.range.endInclusive, 12)
    }

    @Test fun testCanonEqFlagWithSupplementaryCharacters() {

        /*
         * \u1D1BF->\u1D1BB\u1D16F->\u1D1B9\u1D165\u1D16F in UTF32
         * \uD834\uDDBF->\uD834\uDDBB\uD834\uDD6F
         * ->\uD834\uDDB9\uD834\uDD65\uD834\uDD6F in UTF16
         */
        var patString = "abc\uD834\uDDBFef"
        var testString = "abc\uD834\uDDB9\uD834\uDD65\uD834\uDD6Fef"
        var regex = Regex(patString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))

        testString = "abc\uD834\uDDBB\uD834\uDD6Fef"
        assertTrue(regex.matches(testString))

        patString = "abc\uD834\uDDBB\uD834\uDD6Fef"
        testString = "abc\uD834\uDDBFef"
        regex = Regex(patString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))

        testString = "abc\uD834\uDDB9\uD834\uDD65\uD834\uDD6Fef"
        assertTrue(regex.matches(testString))

        patString = "abc\uD834\uDDB9\uD834\uDD65\uD834\uDD6Fef"
        testString = "abc\uD834\uDDBFef"
        regex = Regex(patString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))

        testString = "abc\uD834\uDDBB\uD834\uDD6Fef"
        assertTrue(regex.matches(testString))

        /*
         * testSupplementary characters with no decomposition
         */
        patString = "a\uD9A0\uDE8Ebc\uD834\uDDBB\uD834\uDD6Fe\uDE8Ef"
        testString = "a\uD9A0\uDE8Ebc\uD834\uDDBFe\uDE8Ef"
        regex = Regex(patString, RegexOption.CANON_EQ)
        assertTrue(regex.matches(testString))
    }

    @Test fun testRangesWithSurrogatesSupplementary() {
        var patString = "[abc\uD8D2]"
        var testString = "\uD8D2"
        var regex = Regex(patString)
        assertTrue(regex.matches(testString))

        testString = "a"
        assertTrue(regex.matches(testString))

        testString = "ef\uD8D2\uDD71gh"
        assertFalse(regex.containsMatchIn(testString))

        testString = "ef\uD8D2gh"
        assertTrue(regex.containsMatchIn(testString))

        patString = "[abc\uD8D3&&[c\uD8D3]]"
        testString = "c"
        regex = Regex(patString)
        assertTrue(regex.matches(testString))

        testString = "a"
        assertFalse(regex.matches(testString))

        testString = "ef\uD8D3\uDD71gh"
        assertFalse(regex.containsMatchIn(testString))

        testString = "ef\uD8D3gh"
        assertTrue(regex.containsMatchIn(testString))

        patString = "[abc\uD8D3\uDBEE\uDF0C&&[c\uD8D3\uDBEE\uDF0C]]"
        testString = "c"
        regex = Regex(patString)
        assertTrue(regex.matches(testString))

        testString = "\uDBEE\uDF0C"
        assertTrue(regex.matches(testString))

        testString = "ef\uD8D3\uDD71gh"
        assertFalse(regex.containsMatchIn(testString))

        testString = "ef\uD8D3gh"
        assertTrue(regex.containsMatchIn(testString))

        patString = "[abc\uDBFC]\uDDC2cd"
        testString = "\uDBFC\uDDC2cd"
        regex = Regex(patString)
        assertFalse(regex.matches(testString))

        testString = "a\uDDC2cd"
        assertTrue(regex.matches(testString))
    }

    @Test fun testSequencesWithSurrogatesSupplementary() {
        var patString = "abcd\uD8D3"
        var testString = "abcd\uD8D3\uDFFC"
        var regex = Regex(patString)
        assertFalse(regex.containsMatchIn(testString))

        testString = "abcd\uD8D3abc"
        assertTrue(regex.containsMatchIn(testString))

        patString = "ab\uDBEFcd"
        testString = "ab\uDBEFcd"
        regex = Regex(patString)
        assertTrue(regex.matches(testString))

        patString = "\uDFFCabcd"
        testString = "\uD8D3\uDFFCabcd"
        regex = Regex(patString)
        assertFalse(regex.containsMatchIn(testString))

        testString = "abc\uDFFCabcdecd"
        assertTrue(regex.containsMatchIn(testString))

        patString = "\uD8D3\uDFFCabcd"
        testString = "abc\uD8D3\uD8D3\uDFFCabcd"
        regex = Regex(patString)
        assertTrue(regex.containsMatchIn(testString))
    }

    @Test fun testPredefinedClassesWithSurrogatesSupplementary() {
        var patString = "[123\\D]"
        var testString = "a"
        var regex = Regex(patString)
        assertTrue(regex.containsMatchIn(testString))

        testString = "5"
        assertFalse(regex.containsMatchIn(testString))

        testString = "3"
        assertTrue(regex.containsMatchIn(testString))

        // low surrogate
        testString = "\uDFC4"
        assertTrue(regex.containsMatchIn(testString))

        // high surrogate
        testString = "\uDADA"
        assertTrue(regex.containsMatchIn(testString))

        testString = "\uDADA\uDFC4"
        assertTrue(regex.containsMatchIn(testString))

        testString = "5"
        assertFalse(regex.containsMatchIn(testString))

        testString = "3"
        assertTrue(regex.containsMatchIn(testString))

        // low surrogate
        testString = "\uDFC4"
        assertTrue(regex.containsMatchIn(testString))

        // high surrogate
        testString = "\uDADA"
        assertTrue(regex.containsMatchIn(testString))

        testString = "\uDADA\uDFC4"
        assertTrue(regex.containsMatchIn(testString))

        // surrogate characters
        patString = "\\p{Cs}"
        testString = "\uD916\uDE27"
        regex = Regex(patString)
        /*
     * see http://www.unicode.org/reports/tr18/#Supplementary_Characters we
     * have to treat text as code points not code units. \\p{Cs} matches any
     * surrogate character but here testString is a one code point
     * consisting of two code units (two surrogate characters) so we find
     * nothing
     */
        assertFalse(regex.containsMatchIn(testString))

        // swap low and high surrogates
        testString = "\uDE27\uD916"
        assertTrue(regex.containsMatchIn(testString))

        patString = "[\uD916\uDE271\uD91623&&[^\\p{Cs}]]"
        testString = "1"
        regex = Regex(patString)
        assertTrue(regex.containsMatchIn(testString))

        testString = "\uD916"
        regex = Regex(patString)
        assertFalse(regex.containsMatchIn(testString))

        testString = "\uD916\uDE27"
        regex = Regex(patString)
        assertTrue(regex.containsMatchIn(testString))

        // \uD9A0\uDE8E=\u7828E
        // \u78281=\uD9A0\uDE81
        patString = "[a-\uD9A0\uDE8E]"
        testString = "\uD9A0\uDE81"
        regex = Regex(patString)
        assertTrue(regex.matches(testString))
    }

    @Test fun testDotConstructionWithSurrogatesSupplementary() {
        var patString = "."
        var testString = "\uD9A0\uDE81"
        var regex = Regex(patString)
        assertTrue(regex.matches(testString))

        testString = "\uDE81"
        assertTrue(regex.matches(testString))

        testString = "\uD9A0"
        assertTrue(regex.matches(testString))

        testString = "\n"
        assertFalse(regex.matches(testString))

        patString = ".*\uDE81"
        testString = "\uD9A0\uDE81\uD9A0\uDE81\uD9A0\uDE81"
        regex = Regex(patString)
        assertFalse(regex.matches(testString))

        testString = "\uD9A0\uDE81\uD9A0\uDE81\uDE81"
        assertTrue(regex.matches(testString))

        patString = ".*"
        testString = "\uD9A0\uDE81\n\uD9A0\uDE81\uD9A0\n\uDE81"
        regex = Regex(patString, RegexOption.DOT_MATCHES_ALL)
        assertTrue(regex.matches(testString))
    }

    @Test fun testQuantifiersWithSurrogatesSupplementary() {
        val patString = "\uD9A0\uDE81*abc"
        var testString = "\uD9A0\uDE81\uD9A0\uDE81abc"
        val regex = Regex(patString)
        assertTrue(regex.matches(testString))

        testString = "abc"
        assertTrue(regex.matches(testString))
    }

    @Test fun testAlternationsWithSurrogatesSupplementary() {
        val patString = "\uDE81|\uD9A0\uDE81|\uD9A0"
        var testString = "\uD9A0"
        val regex = Regex(patString)
        assertTrue(regex.matches(testString))

        testString = "\uDE81"
        assertTrue(regex.matches(testString))

        testString = "\uD9A0\uDE81"
        assertTrue(regex.matches(testString))

        testString = "\uDE81\uD9A0"
        assertFalse(regex.matches(testString))
    }

    @Test fun testGroupsWithSurrogatesSupplementary() {

        // this pattern matches nothing
        var patString = "(\uD9A0)\uDE81"
        var testString = "\uD9A0\uDE81"
        var regex = Regex(patString)
        assertFalse(regex.matches(testString))

        patString = "(\uD9A0)"
        testString = "\uD9A0\uDE81"
        regex = Regex(patString, RegexOption.DOT_MATCHES_ALL)
        assertFalse(regex.containsMatchIn(testString))
    }
}
