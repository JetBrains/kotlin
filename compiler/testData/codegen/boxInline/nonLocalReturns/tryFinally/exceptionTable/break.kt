// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

public inline fun doCallAlwaysBreak(block: (i: Int)-> Int) : Int {
    var res = 0;
    for (i in 1..10) {
        try {
            block(i)
        } finally {
            break;
        }
    }
    return res
}

public val z: Boolean = true

public inline fun doCallAlwaysBreak2(block: (i: Int)-> Int) : Int {
    var res = 0;
    for (i in 1..10) {
        try {
            res = block(i)
        } finally {
            if (z)
                break
        }
    }
    return res
}

//public inline fun doCallAlwaysBreak2(block: (i: Int)-> Int) : Int {
//    var res = 0;
//    for (i in 1..10) {
//        try {
//            res += block(i)
//        } finally {
//            if (z)
//                break
//        }
//    }
//    return res
//}

// FILE: 2.kt

import test.*

fun test1(): Int {
    var s = 0
    doCallAlwaysBreak {
        s += it*it
        s
    }
    return s;
}

fun test11(): Int {
    return doCallAlwaysBreak {
        return -100
    }
}

fun test2(): Int {
    return doCallAlwaysBreak2 {
        return -100
    }
}

fun test22(): Int {
    var s = 0
    doCallAlwaysBreak {
        s += it*it
        s
    }
    return s;
}



fun box(): String {
    val test1 = test1()
    if (test1 != 1) return "test1: ${test1}"

    val test11 = test11()
    if (test11 != 0) return "test11: ${test11}"

    val test2 = test2()
    if (test2 != 0) return "test2: ${test2}"

    val test22 = test22()
    if (test22 != 1) return "test22: ${test22}"

    return "OK"
}
