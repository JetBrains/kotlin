// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

public inline fun <R> doCall(block: ()-> R) : R {
    return block()
}

// FILE: 2.kt

import test.*

fun test1(nonLocal: String): String {
    val localResult = doCall<String> {
        return nonLocal
    }

    return "NON_LOCAL_FAILED"
}


fun box(): String {
    return test1("OK")
}
