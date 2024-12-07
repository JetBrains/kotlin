// FILE: main.kt
package test

import dep2.*
import dep1.*

/**
 * [<caret_1>Foo]
 *
 * [dep1.<caret_2>Foo]
 * [dep2.<caret_3>Foo]
 */
fun test() {}

// FILE: dep1.kt
package dep1

class Foo

// FILE: dep2.kt
package dep2

class Foo

