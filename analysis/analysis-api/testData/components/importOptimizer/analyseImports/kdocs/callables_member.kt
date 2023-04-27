// FILE: main.kt
package test

import dependency.MyClass

/**
 * [MyClass.function]
 * [MyClass.property]
 */
fun usage() {}

// FILE: dependency.kt
package dependency

class MyClass {
    fun function() {}
    val property: Int = 0
}