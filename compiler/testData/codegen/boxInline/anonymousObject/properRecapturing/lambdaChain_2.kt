// IGNORE_INLINER_K2: IR
// FILE: 1.kt

package test

inline fun <T> inlineFun(arg: T, crossinline f: (T) -> Unit) {
    {
        f(arg)
    }.let { it() }
}

// FILE: 2.kt

import test.*

fun box(): String {
    val param = "start"
    var result = "fail"
    inlineFun("1")  { c ->
        {
            inlineFun("2") { a ->
                {
                    {
                        result = param + c + a
                    }.let { it() }
                }.let { it() }
            }
        }.let { it() }
    }

    return if (result == "start12") "OK" else "fail: $result"
}
