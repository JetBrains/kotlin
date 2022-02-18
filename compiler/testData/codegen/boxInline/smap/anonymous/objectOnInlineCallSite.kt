// IGNORE_BACKEND_MULTI_MODULE: JVM_IR, JVM_MULTI_MODULE_IR_AGAINST_OLD
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND_FIR_MULTI_MODULE: JVM_IR
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package builders
inline fun call(crossinline init: () -> Unit) {
    return init()
}

// FILE: 2.kt

import builders.*


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


fun box(): String {
    return test()
}
