// IGNORE_INLINER: IR

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

val FINALLY_CHAIN = "in local finally, in doCall finally, in external finally, in doCall finally, in global finally"

fun test1(holder: Holder, p: Int): String {
    holder.value = "start"
    return test2(holder) { i ->
        if (p == i) {
            return "call $i"
        }
    }
}

inline fun test2(holder: Holder, l: (s: Int) -> Unit): String {
    try {
        l(0)
        var externalResult = doCall (ext@ {
            l(1)
            try {
                l(2)
                val internalResult = doCall (int@ {
                    l(3)
                    try {
                        l(4)
                        return "fail"
                    }
                    finally {
                        holder.value += ", in local finally"
                    }
                }, holder)
            }
            finally {
                holder.value += ", in external finally"
            }
        }, holder)

        return "fail"
    }
    finally {
        holder.value += ", in global finally"
    }
}

fun box(): String {
    var holder = Holder()

    var test1 = test1(holder, 0)
    if (holder.value != "start, in global finally" || test1 != "call 0") return "test1: ${test1},  finally = ${holder.value}"

    test1 = test1(holder, 1)
    if (holder.value != "start, in doCall finally, in global finally" || test1 != "call 1")
        return "test2: ${test1},  finally = ${holder.value}"

    test1 = test1(holder, 2)
    if (holder.value != "start, in external finally, in doCall finally, in global finally" || test1 != "call 2")
        return "test3: ${test1},  finally = ${holder.value}"


    test1 = test1(holder, 3)
    if (holder.value != "start, in doCall finally, in external finally, in doCall finally, in global finally" || test1 != "call 3")
        return "test4: ${test1},  finally = ${holder.value}"

    test1 = test1(holder, 4)
    if (holder.value != "start, in local finally, in doCall finally, in external finally, in doCall finally, in global finally" || test1 != "call 4")
        return "test5: ${test1},  finally = ${holder.value}"

    return "OK"
}
