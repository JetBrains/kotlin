// NO_CHECK_LAMBDA_INLINING

// FILE: 1.kt
package test

inline fun <T> foo(operation: () -> T): T = operation()

class Shape(val name: String)

class Registry {
    fun hasIntersectingObject(o: Shape): Boolean {
        return true
    }
}

fun test(): String {
    return foo {
        val registry = Registry()

        fun localFunction(shape: Shape): Boolean {
            return registry.hasIntersectingObject(shape)
        }

        Shape("OK").takeIf(::localFunction)?.name ?: "Fail"
    }
}

// FILE: 2.kt

import test.*

fun box(): String {
    return test()
}
