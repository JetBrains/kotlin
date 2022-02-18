// FULL_JDK
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND_FIR_MULTI_MODULE: JVM_IR
// TARGET_BACKEND: JVM
// FILE: 1.kt

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
