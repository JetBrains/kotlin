// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

public inline fun <R> mysynchronized(lock: Any, block: () -> R): R {
    try {
        return block()
    }
    finally {
        //do nothing
        1
    }
}

// FILE: 2.kt

import test.*

fun call(): String {
    return nonLocal()
}

inline fun nonLocal(): String {
    mysynchronized("__LOCK__") {
        return "nonLocal"
    }
    return "local"
}

fun box(): String {
    val call = call()
    if (call != "nonLocal") return "fail $call"
    return "OK"
}
