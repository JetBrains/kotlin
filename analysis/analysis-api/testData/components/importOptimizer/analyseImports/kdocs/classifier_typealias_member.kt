// FILE: main.kt
package test

import dependency.MyTypealias

/**
 * [MyTypealias.function]
 */
fun usage() {}

// FILE: dependency.kt
package dependency

class MyClass {
    fun function() {}
}

typealias MyTypealias = MyClass