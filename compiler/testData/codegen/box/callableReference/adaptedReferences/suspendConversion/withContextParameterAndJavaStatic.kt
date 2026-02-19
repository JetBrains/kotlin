// TARGET_BACKEND: JVM
// WITH_COROUTINES
// WITH_STDLIB
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// FILE: JavaClass.java
public class JavaClass {
    static String result = "FAIL";
    public static void foo(Integer x) {
        result = "OK";
    }
}

// FILE: test.kt
import helpers.*
import kotlin.coroutines.*

fun runSuspend(c: suspend context(Int) () -> Unit) {
    c.startCoroutine(1, EmptyContinuation)
}

fun box(): String {
    runSuspend(JavaClass::foo)
    return JavaClass.result
}