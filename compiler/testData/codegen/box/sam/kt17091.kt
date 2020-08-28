// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: Foo.kt
package foo

class A2 {
    fun doWork(job: () -> Unit) {
        Runnable(job)
    }
}

// FILE: kt17091.kt
import foo.A2

typealias Z = String

class A {
    fun doWork(job: () -> Unit) {
        java.lang.Runnable(job).run()
    }
}

fun box(): String {
    var result = "fail"
    A().doWork { result = "OK" }

    if (java.lang.Class.forName("Kt17091Kt\$sam\$java_lang_Runnable$0") == null) return "fail: can't find sam wrapper"

    if (java.lang.Class.forName("foo.A2\$sam\$java_lang_Runnable$0") == null) return "fail 2: can't find sam wrapper"

    return result
}