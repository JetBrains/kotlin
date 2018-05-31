/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package runtime.text.indexof

import kotlin.test.*

@Test fun runTest() {
    var str = "Hello World!!"    // for indexOf String
    var ch = 'a'                 // for indexOf Char

    assertEquals(6, str.indexOf("World", 0))
    assertEquals(6, str.indexOf("World", -1))

    assertEquals(-1, str.indexOf(ch, 0))

    str = "Kotlin/Native"
    assertEquals(-1, str.indexOf("/", str.length + 1))
    assertEquals(-1, str.indexOf("/", Int.MAX_VALUE))
    assertEquals(str.length, str.indexOf("", Int.MAX_VALUE))
    assertEquals(1, str.indexOf("", 1))

    assertEquals(8, str.indexOf(ch, 1))
    assertEquals(-1, str.indexOf(ch, str.length - 1))

    str = ""
    assertEquals(-1, str.indexOf("a", -3))
    assertEquals(0, str.indexOf("", 0))

    assertEquals(-1, str.indexOf(ch, -3))
    assertEquals(-1, str.indexOf(ch, 10))

    ch = 0.toChar()
    assertEquals(-1, str.indexOf(ch, -3))
    assertEquals(-1, str.indexOf(ch, 10))
}