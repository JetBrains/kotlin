// FILE: main.kt
package test

import dependency.MyClass
import dependency.MyTypealias

/**
 * [MyTypealias]
 */
fun usage() {}

// FILE: dependency.kt
package dependency

class MyClass

typealias MyTypealias = MyClass