// NO_CHECK_LAMBDA_INLINING
// IGNORE_INLINER_K2: IR
// FILE: 1.kt

package builders

inline fun call(crossinline init: () -> Unit) {
    val lambda = {
        init()
    }; lambda()
}

// FILE: 2.kt

import builders.*

fun test(): String {
    var res = "Fail"

    call {
        res = "OK"
    }

    return res
}


fun box(): String {
    return test()
}
