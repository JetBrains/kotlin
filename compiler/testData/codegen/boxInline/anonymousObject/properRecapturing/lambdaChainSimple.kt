// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

inline fun <T> inlineFun(arg: T, crossinline f: (T) -> Unit) {
    {
        f(arg)
    }()
}

// FILE: 2.kt

import test.*

fun box(): String {
    val param = "start"
    var result = "fail"

    inlineFun("2") { a ->
        {
            result = param + a
        }()
    }


    return if (result == "start2") "OK" else "fail: $result"
}
