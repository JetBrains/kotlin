// IGNORE_BACKEND_K2_MULTI_MODULE: JS_IR, WASM_JS, JVM_IR_SERIALIZE
// FILE: 1.kt
package test.o

import test.I

inline fun run(): String {
    return object : I {
        override fun run() = "O"
    }.run()
}


// FILE: 2.kt
package test.k

import test.I

inline fun run(): String {
    return object : I {
        override fun run() = "K"
    }.run()
}

// FILE: 3.kt

// CHECK_BREAKS_COUNT: function=ok count=0
// CHECK_LABELS_COUNT: function=ok name=$l$block count=0
package test

fun ok() = test.o.run() + test.k.run()

// FILE: main.kt
// RECOMPILE
package test

interface I {
    fun run(): String
}

fun box(): String {

    if (ok() != "OK") return "fail"

    return test.o.run() + test.k.run()
}