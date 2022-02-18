// IGNORE
// NO_CHECK_LAMBDA_INLINING
// IGNORE_BACKEND_MULTI_MODULE: JVM, JVM_IR, JVM_MULTI_MODULE_OLD_AGAINST_IR, JVM_MULTI_MODULE_IR_AGAINST_OLD
// IGNORE_BACKEND_FIR_MULTI_MODULE: JVM_IR
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
