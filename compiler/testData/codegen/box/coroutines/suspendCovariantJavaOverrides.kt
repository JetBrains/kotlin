// TARGET_BACKEND: JVM
// WITH_STDLIB
// WITH_COROUTINES

// FILE: I.kt

interface I {
    suspend fun foo(x: Int): String
    suspend fun bar(x: Int): String
}

// FILE: JavaClass.java

public class JavaClass implements I {
    @Override
    public String foo(int x, kotlin.coroutines.Continuation<? super String> continuation) {
        return "O";
    }

    @Override
    public Object bar(int x, kotlin.coroutines.Continuation<? super String> continuation) {
        return foo(x, continuation);
    }
}

// FILE: main.kt
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class K : JavaClass() {
    override suspend fun foo(x: Int): String = super.foo(x) + suspendCoroutine { it.resume("K") }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = "fail"

    builder {
        // Changing the call to 'K().bar(1)' doesn't work because of KT-25036
        result = K().foo(1)
    }

    return result
}
