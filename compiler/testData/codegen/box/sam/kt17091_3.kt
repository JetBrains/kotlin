// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: Foo.kt
@file:JvmMultifileClass
@file:JvmName("testX")
package test

class A2 {
    fun doWork(job: () -> Unit) {
        Runnable(job)
    }
}


// FILE: kt17091_3.kt

fun box(): String {
    if (java.lang.Class.forName("Kt17091_3Kt\$sam\$java_util_concurrent_Callable$0") == null) return "fail: can't find sam wrapper"

    if (java.lang.Class.forName("test.A2\$sam\$java_lang_Runnable$0") == null) return "fail 2: can't find sam wrapper"

    return A().foo().call()
}

class A {
    val f = {"OK"}
    fun foo() = java.util.concurrent.Callable(f)
}