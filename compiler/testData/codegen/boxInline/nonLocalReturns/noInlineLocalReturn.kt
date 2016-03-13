// FILE: 1.kt

package test

public fun <R> noInlineCall(block: ()-> R) : R {
    return block()
}

public inline fun <R> notUsed(block: ()-> R) : R {
    return block()
}

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
import test.*

fun test1(b: Boolean): String {
    val localResult = noInlineCall local@ {
        if (b) {
            return@local 1
        } else {
            return@local 2
        }
        3
    }

    return "result=" + localResult;
}

fun box(): String {
    val test1 = test1(true)
    if (test1 != "result=1") return "test1: ${test1}"

    val test2 = test1(false)
    if (test2 != "result=2") return "test2: ${test2}"

    return "OK"
}
