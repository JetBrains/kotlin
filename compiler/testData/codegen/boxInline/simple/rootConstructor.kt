// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test


inline fun <R> doWork(crossinline job: ()-> R) : R {
    return notInline({job()})
}

fun <R> notInline(job: ()-> R) : R {
    return job()
}

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
import  test.*

val s = doWork({11})

fun box(): String {
    if (s != 11) return "test1: ${s}"

    return "OK"
}
