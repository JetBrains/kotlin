// FILE: 1.kt

package test

class Measurements
{
    inline fun measure(key: String, logEvery: Long = -1, divisor: Int = 1, body: () -> Unit): String {
        body()
        return key
    }
}

// FILE: 2.kt

import test.*

fun box(): String {
    var s1 = ""
    val s2 = Measurements().measure("K") {
        s1 = "O"
    }

    return s1 + s2
}
