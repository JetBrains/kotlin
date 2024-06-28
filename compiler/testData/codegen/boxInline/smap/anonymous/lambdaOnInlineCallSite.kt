// IGNORE_BACKEND_MULTI_MODULE: JVM_IR, JVM_MULTI_MODULE_IR_AGAINST_OLD, JVM_IR_SERIALIZE
// NO_CHECK_LAMBDA_INLINING
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_K2_MULTI_MODULE: JVM_IR JVM_IR_SERIALIZE
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
        {
            res = "OK"
        }()
    }

    return res
}


fun box(): String {
    return test()
}
