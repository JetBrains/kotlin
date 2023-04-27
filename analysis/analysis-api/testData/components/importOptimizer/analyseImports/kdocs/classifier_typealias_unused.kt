// FILE: main.kt
package test

import dependency.MyTypealias

/**
 * [dependency.MyTypealias]
 */
fun usage() {}

// FILE: dependency.kt
package dependency

class MyClass

typealias MyTypealias = MyClass