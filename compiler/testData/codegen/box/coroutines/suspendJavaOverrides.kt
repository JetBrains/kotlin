// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST

// FILE: I.kt

interface I {
    suspend fun foo(x: Int): String
}

// FILE: JavaClass.java

public class JavaClass implements I {
    @Override
    public Object foo(int x, COROUTINES_PACKAGE.Continuation<? super String> continuation) {
        continuation.resume("OK");
        return COROUTINES_PACKAGE.intrinsics.IntrinsicsKt.getCOROUTINE_SUSPENDED();
    }
}

// FILE: main.kt
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = "fail"

    builder {
        result = JavaClass().foo(1)
    }

    return result
}
