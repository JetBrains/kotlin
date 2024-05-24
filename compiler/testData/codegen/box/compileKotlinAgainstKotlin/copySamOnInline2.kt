// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// MODULE: lib
// FILE: A.kt

package test

import java.util.concurrent.Callable

inline fun doWork(noinline job: () -> String): Callable<String> {
    return Callable(job)
}

// MODULE: main(lib)
// FILE: B.kt

import test.*

fun box(): String {
    val anotherModule = doWork { "K" }

    if (anotherModule.javaClass.name != "BKt\$inlined\$sam\$i\$java_util_concurrent_Callable\$0") return "class should be regenerated, but ${anotherModule.javaClass.name}"

    return "OK"
}
