// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt


package builders

inline fun call(crossinline init: () -> Unit) {
    return object {
        fun run () {
            init()
        }
    }.run()
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
