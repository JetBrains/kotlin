// LANGUAGE_VERSION: 1.2
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// WITH_COROUTINES

// FILE: I.kt

interface I {
    suspend fun foo(x: Int): String
}

// FILE: JavaClass.java

public class JavaClass implements I {
    @Override
    public Object foo(int x, kotlin.coroutines.experimental.Continuation<? super String> continuation) {
        continuation.resume("OK");
        return kotlin.coroutines.experimental.intrinsics.IntrinsicsKt.getCOROUTINE_SUSPENDED();
    }
}

// FILE: main.kt
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

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
