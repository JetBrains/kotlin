// FILE: 1.kt

package test

inline fun <R> mySynchronized(lock: Any, block: () -> R): R {
    monitorCall(lock)
    try {
        return block()
    }
    finally {
        monitorCall(lock)
    }
}

fun monitorCall(lock: Any) {

}

// FILE: 2.kt

import test.*

public class ClassA {
    val LOCK = "__LOCK__"

    var result = "fail"

    fun test(name1: String?, name2: String, cond: Boolean) {
        mySynchronized (LOCK) {
            var name = name1

            if (name == null) {
                if (cond) {
                    result = "NLR" + name2
                    return
                }

                name = name2
            }

            result = name + name2

            val length = name.length
        }
    }
}

fun box(): String {
    val classA = ClassA()
    classA.test(null, "2", true)
    if (classA.result != "NLR2") return "fail 1: ${classA.result}"

    classA.test(null, "K", false)
    if (classA.result != "KK") return "fail 1: ${classA.result}"


    classA.test("O", "K", false)
    return classA.result
}
