// IGNORE_INLINER: IR
// IGNORE
// NO_CHECK_LAMBDA_INLINING
// SEPARATE_SMAP_DUMPS
// FILE: 1.kt
// NO_SMAP_DUMP

package builders

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

import builders.*


fun box(): String {
    return test()
}
