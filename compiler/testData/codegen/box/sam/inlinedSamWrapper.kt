// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: MyRunnable.java
public interface MyRunnable {
    public void run();
}

// FILE: Foo.kt
package foo
import MyRunnable

class A {
    inline fun doWork(noinline job: () -> Unit) {
        Runnable(job).run()
    }

    fun doNoninlineWork(job: () -> Unit) {
        Runnable(job).run()
    }
}

class B {
    inline fun doWork(noinline job: () -> Unit) {
        Runnable(job).run()
    }

    fun doNonInlineWork(job: () -> Unit) {
        MyRunnable(job).run()
    }
}

// FILE: test.kt
import foo.A
import foo.B

fun classForName(name: String): Class<*>? =
    try {
        java.lang.Class.forName(name)
    } catch (e: Throwable) {
        null
    }

fun box(): String {
    var result = false
    A().doWork { result = true }
    if (!result) return "Fail 1"

    result = false
    A().doNoninlineWork { result = true }
    if (!result) return "Fail 2"

    result = false
    B().doWork { result = true }
    if (!result) return "Fail 3"

    result = false
    B().doNonInlineWork { result = true }
    if (!result) return "Fail 4"

    val inlineWrapperA = classForName("foo.A\$sam\$i\$java_lang_Runnable$0")
    if (inlineWrapperA == null) return "Fail 5: Can't find sam wrapper"
    if (inlineWrapperA.modifiers and 1 == 0) return "Fail 6: inline sam wrapper is non-public"

    val wrapperA = classForName("foo.A\$sam\$java_lang_Runnable$0")
    if (wrapperA == null) return "Fail 7: Can't find sam wrapper"
    if (wrapperA.modifiers and 1 != 0) return "Fail 8: non-inline sam wrapper is public"

    val inlineWrapperB = classForName("foo.B\$sam\$i\$java_lang_Runnable$0")
    if (inlineWrapperB != null) return "Fail 9: sam wrapper not cached"

    val wrapperB = classForName("foo.B\$sam\$MyRunnable$0")
    if (wrapperB == null) return "Fail 10: Can't find sam wrapper"
    if (wrapperB.modifiers and 1 != 0) return "Fail 11: non-inline sam wrapper is public"

    return "OK"
}
