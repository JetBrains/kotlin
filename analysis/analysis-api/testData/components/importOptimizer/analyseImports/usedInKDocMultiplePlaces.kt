// FILE: main.kt
package usage

import pkg1.foo
import pkg2.foo

/**
 * [foo]
 */
fun test() {}

// FILE: dependency.kt
package pkg1

fun foo() {}

// FILE: dependency2.kt
package pkg2

fun foo() {}