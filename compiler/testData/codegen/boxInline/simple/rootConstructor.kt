// NO_CHECK_LAMBDA_INLINING
// IGNORE_INLINER_K2: IR
// FILE: 1.kt

package test


inline fun <R> doWork(crossinline job: ()-> R) : R {
    return notInline({job()})
}

fun <R> notInline(job: ()-> R) : R {
    return job()
}

// FILE: 2.kt

import  test.*

val s = doWork({11})

fun box(): String {
    if (s != 11) return "test1: ${s}"

    return "OK"
}
