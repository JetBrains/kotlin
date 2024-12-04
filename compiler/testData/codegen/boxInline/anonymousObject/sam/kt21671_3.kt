// FULL_JDK
// TARGET_BACKEND: JVM
// FILE: 1.kt

package test
import java.util.concurrent.Executors

inline fun doWork(noinline job: ()-> Unit) {
    { Executors.callable(job).call() }.let { it() }
    Executors.callable(job).call()
}

// FILE: 2.kt

import test.*

fun box() : String {
    var result = ""
    var value = 1
    doWork { result += if (value++ == 1) "O" else "K" }

    return result
}
