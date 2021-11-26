// WITH_STDLIB
// FILE: 1.kt
package test

inline fun myRun( x: () -> String): Lazy<String> {
    val value2 = x()
    return object : Lazy<String> {
        override val value: String
            get() = value2

        override fun isInitialized(): Boolean = true
    }
}

// FILE: 2.kt

import test.*

class C {
    val x: String
    init {
        val y by myRun { { "OK" }() }
        x = y
    }

    constructor(y: Int)
    constructor(y: String)
}

fun box(): String = C("").x
