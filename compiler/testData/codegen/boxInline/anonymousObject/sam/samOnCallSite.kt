// FULL_JDK
// NO_CHECK_LAMBDA_INLINING
// TARGET_BACKEND: JVM
// FILE: 1.kt

package test

inline fun doWork(job: ()-> Unit) {
    job()
}

// FILE: 2.kt

import test.*
import java.util.concurrent.Executors

fun box() : String {
    var result = "fail"
    doWork {
        val job = { result = "OK" }
        Executors.callable(job).call()
    }

    return result
}
