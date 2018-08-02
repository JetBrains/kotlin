// FILE: 1.kt
// IGNORE_BACKEND: JVM_IR
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
//TODO SHOULD BE LESS

// FILE: 2.kt

import builders.*


fun box(): String {
    return test{"OK"}
}
//NO_CHECK_LAMBDA_INLINING

//TODO
