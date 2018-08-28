// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

public inline fun doCall(block: (i: Int)-> Int, fblock: (i: Int)-> Unit) : Int {
    var res = 0;
    for (i in 1..10) {
        try {
            res = block(i)
        } finally {
            for (i in 1..10) {
                fblock(i)
            }
        }
    }
    return res
}

// FILE: 2.kt

import test.*

class Holder {
    var value: Int = 0
}

fun test1(): Int {
    var s = 0
    doCall (
            {
                s += it * it
                s
            },
            {
                s += it
            }
    )
    return s;
}

fun test11(h: Holder): Int {
    return doCall (
            {
                return -100
            }, {
                h.value += it
            })
}


fun box(): String {
    val test1 = test1()
    if (test1 != 935) return "test1: ${test1}"

    val h = Holder()
    val test11 = test11(h)
    if (test11 != -100 && h.value != 55) return "test11: ${test11} holder: ${h.value}"

    return "OK"
}
