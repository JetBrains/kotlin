// FILE: 1.kt

package test

inline fun myrun(s: () -> Unit) {
    val z = "myrun"
    s()
}

inline fun test(crossinline s: () -> Unit) {
    {
        val z = 1;
        myrun(s)
        val x = 1;
    }()
}

// FILE: 2.kt

import test.*

fun box(): String {
    var result = "fail"

    test {
        result = "OK"
    }

    return result
}
