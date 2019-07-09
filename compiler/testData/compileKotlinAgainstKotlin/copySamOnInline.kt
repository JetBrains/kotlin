// IGNORE_BACKEND: JVM_IR
// FILE: A.kt
// FULL_JDK

package test

import java.util.concurrent.Callable

class A(val callable: Callable<String>)

inline fun doWork(noinline job: () -> String): Callable<String> {
    val a = A(Callable(job))
    return a.callable
}

var sameModule = doWork { "O" }


// FILE: B.kt

import test.*

fun box(): String {
    val anotherModule = doWork { "K" }

    if (sameModule.javaClass.name == anotherModule.javaClass.name) return "class should be regenerated, but ${anotherModule.javaClass.name}"
    if (sameModule.javaClass.name.contains("inlined")) return "Sam in same module shouldn't be copied, but ${sameModule.javaClass.name}"
    if (!anotherModule.javaClass.name.contains("inlined")) return "Sam in another module should be copied, but ${sameModule.javaClass.name}"

    return sameModule.call() + anotherModule.call()
}
