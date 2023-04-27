// FILE: main.kt
package test

import dependency1.function
import dependency2.function

/**
 * [function]
 */
fun usage() {}

// FILE: dependency1.kt
package dependency1

fun function(s: String) {}

// FILE: dependency2.kt
package dependency2

fun function(s: String) {}
