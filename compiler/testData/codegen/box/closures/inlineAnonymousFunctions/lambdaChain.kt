// FILE: 1.kt

package test

inline fun <T> inlineFun(arg: T, f: (T) -> Unit) {
    f(arg)
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
                    }()
                }()
            }
        }()
    }

    return if (result == "start12") "OK" else "fail: $result"
}
