// FILE: 1.kt

package test

inline fun call(crossinline init: () -> Unit) {
    return init()
}

inline fun test(): String {
    var res = "Fail"

    call {
        object {
            fun run () {
                res = "OK"
            }
        }.run()
    }

    return res
}

// FILE: 2.kt

import test.*

fun box(): String {
    return test()
}
