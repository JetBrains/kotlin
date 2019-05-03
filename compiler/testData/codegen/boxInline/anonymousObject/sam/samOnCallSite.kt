// FILE: 1.kt
// FULL_JDK

package test

inline fun doWork(job: ()-> Unit) {
    job()
}

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
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
