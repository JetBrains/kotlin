// FULL_JDK
// NO_CHECK_LAMBDA_INLINING
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND_FIR_MULTI_MODULE: JVM_IR JVM_IR_SERIALIZE
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
