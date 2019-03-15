// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
// FULL_JDK

package test
import java.util.concurrent.Executors

inline fun doWork(noinline job: ()-> Unit) {
    Executors.callable(job).call()
}

// FILE: 2.kt

import test.*

fun box() : String {
    var result = "fail"
    doWork { result = "OK" }

    return result
}
