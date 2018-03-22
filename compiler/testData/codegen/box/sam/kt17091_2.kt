// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: Foo.kt
@file:JvmName("testXX")
package test

class A2 {
    fun doWork(job: () -> Unit) {
        Runnable(job)
    }
}


// FILE: kt17091_2.kt

@file:JvmMultifileClass
@file:JvmName("testX")
package test

typealias Z = String

class A {
    fun doWork(job: () -> Unit) {
        Runnable(job).run()
    }
}

fun box(): String {
    var result = "fail"
    A().doWork { result = "OK" }

    if (java.lang.Class.forName("test.testX__Kt17091_2Kt\$sam\$java_lang_Runnable$0") == null) return "fail: can't find sam wrapper"

    if (java.lang.Class.forName("test.A2\$sam\$java_lang_Runnable$0") == null) return "fail 2: can't find sam wrapper"

    return result
}