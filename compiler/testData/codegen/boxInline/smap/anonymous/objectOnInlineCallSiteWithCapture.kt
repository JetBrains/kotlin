// NO_CHECK_LAMBDA_INLINING
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_MULTI_MODULE: JVM, JVM_MULTI_MODULE_IR_AGAINST_OLD
// IGNORE_INLINER: IR
// FILE: 1.kt
// NO_SMAP_DUMP
package builders
//TODO there is a bug in asm it's skips linenumber on same line on reading bytecode
inline fun call(crossinline init: () -> Unit) {
    "1"; return init()
}

inline fun test(crossinline p: () -> String): String {
    var res = "Fail"

    call {
        object {
            fun run () {
                res = p()
            }
        }.run()
    }

    return res
}

// FILE: 2.kt

import builders.*


fun box(): String {
    return test{"OK"}
}

