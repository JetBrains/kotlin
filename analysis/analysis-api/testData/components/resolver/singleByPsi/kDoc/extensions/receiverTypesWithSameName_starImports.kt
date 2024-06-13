// FILE: main.kt
package test

import dep2.*
import dep1.*

fun Any.ext() {}

/**
 * [Foo.<caret_1>ext]
 * [<caret_2>Foo.ext]
 *
 * [Foo.<caret_3>depExtShared]
 * [<caret_4>Foo.depExtShared]
 *
 * [Foo.<caret_5>depExt1]
 * [<caret_6>Foo.depExt1]
 *
 * [Foo.<caret_7>depExt2]
 * [<caret_8>Foo.depExt2]
 *
 * [dep1.<caret_9>Foo.ext]
 * [dep2.<caret_10>Foo.ext]
 */
fun test() {}

// FILE: dep1.kt
package dep1

class Foo

fun Foo.depExtShared() {}

fun Foo.depExt1() {}

// FILE: dep2.kt
package dep2

class Foo

fun Foo.depExtShared() {}

fun Foo.depExt2() {}

