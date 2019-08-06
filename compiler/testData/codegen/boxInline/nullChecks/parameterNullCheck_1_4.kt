// !API_VERSION: LATEST
// FILE: 1.kt

package test

public inline fun <R> doRun(block: () -> R): R {
    return block()
}

// FILE: 2.kt

import test.*

fun box(): String {
    return doRun { "OK" }
}
