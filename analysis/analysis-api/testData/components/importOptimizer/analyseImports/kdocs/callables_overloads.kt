// FILE: main.kt
package test

import dependency.function

/**
 * [function]
 */
fun usage() {}

// FILE: dependency.kt
package dependency

fun function() {}
fun function(i: Int) {}
fun function(s: String) {}
