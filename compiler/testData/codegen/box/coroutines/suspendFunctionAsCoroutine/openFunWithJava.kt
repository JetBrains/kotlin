// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
// FILE: main.kt
// TARGET_BACKEND: JVM
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*



open class A(val v: String) {
    suspend fun suspendThere(v: String): String = suspendCoroutineOrReturn { x ->
        x.resume(v)
        COROUTINE_SUSPENDED
    }

    open suspend fun suspendHere(): String = suspendThere("O") + suspendThere(v)
}

class B(v: String) : A(v) {
    override suspend fun suspendHere(): String = super.suspendHere() + suspendThere("56")
}

fun builder(c: suspend A.() -> Unit) {
    c.startCoroutine(B("K"), EmptyContinuation)
}

fun box(): String {
    var result = JavaClass.foo()

    if (result != "OK56") return "fail 1: $result"

    return "OK"
}

// FILE: JavaClass.java
import COROUTINES_PACKAGE.*;
public class JavaClass {
    public static String foo() {
        final String[] res = new String[1];

        new B("K").suspendHere(new Continuation<String>() {
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resume(String s) {
                res[0] = s;
            }

            @Override
            public void resumeWithException(Throwable throwable) {
            }
        });

        return res[0];
    }
}
