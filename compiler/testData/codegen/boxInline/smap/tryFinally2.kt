// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_MULTI_MODULE: JVM_MULTI_MODULE_IR_AGAINST_OLD
// IGNORE_INLINER: IR

// This test is just a cropped copy of `boxInline/nonLocalReturns/tryFinally/chained/nestedLambda.kt`
// FILE: 1.kt

package test

class Holder {
    var value: String = ""
}

inline fun <R> doCall(block: ()-> R, h: Holder) : R {
    try {
        return block()
    } finally {
        h.value += ", in doCall finally"
    }
}

// FILE: 2.kt

import test.*

inline fun test2(holder: Holder, l: (s: Int) -> Unit): String {
    try {
        var externalResult = doCall (ext@ {
            l(1)
        }, holder)

        return "fail"
    }
    finally {
        holder.value += ", in global finally"
    }
}

fun box(): String {
    var holder = Holder()

    return "OK"
}
