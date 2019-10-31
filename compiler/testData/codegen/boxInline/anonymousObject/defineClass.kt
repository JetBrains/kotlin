// FILE: 1.kt

package test

interface Run {
    fun run(): String
}

inline fun i2(crossinline s: () -> String): Run {
    return i1 {
        object : Run {
            override fun run(): String {
                return s()
            }
        }.run()
    }
}

inline fun i1(crossinline s: () -> String): Run {
    return object : Run {
        override fun run(): String {
            return s()
        }
    }
}

// FILE: 2.kt

import test.*

inline fun i4(crossinline s: () -> String): Run {
    return i3 {
        object : Run {
            override fun run(): String {
                return s()
            }
        }.run()
    }
}

inline fun i3(crossinline s: () -> String): Run {
    return i2 {
        object : Run {
            override fun run(): String {
                return s()
            }
        }.run()
    }
}

fun box(): String {
    val i4 = i4 { "OK" }
    return i4.run()
}
