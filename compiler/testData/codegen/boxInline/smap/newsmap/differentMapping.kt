// FILE: 1.kt

package test

inline fun test(s: () -> Unit) {
    val z = 1;
    s()
    val x = 1;
}


// FILE: 2.kt

import test.*

fun box(): String {
    var result = "fail"
    test {
        result = "O"
    }

    test {
        result += "K"
    }

    return result
}
