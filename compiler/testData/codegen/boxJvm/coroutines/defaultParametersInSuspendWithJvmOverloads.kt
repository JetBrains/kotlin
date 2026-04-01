// TARGET_BACKEND: JVM
// WITH_STDLIB
// WITH_COROUTINES

// FILE: defaultParametersInSuspsendWithJvmOverloads.kt
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.test.assertEquals

@JvmOverloads
suspend fun suspendHere(a: String = "abc", i: Int = 2): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(a + "#" + (i + 1))
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    assertEquals(listOf("abc#3", "cde#3", "xyz#10"), J().foo())

    val result = mutableListOf<String>()
    builder {
        result.add(suspendHere())
        result.add(suspendHere("cde"))
        result.add(suspendHere(i = 6))
        result.add(suspendHere("xyz", 9))
    }
    assertEquals(listOf("abc#3", "cde#3", "abc#7", "xyz#10"), result)

    return "OK"
}

// FILE: J.java
import java.util.*;
import kotlin.coroutines.*;

public class J {
    private List<String> result = new ArrayList<String>();

    public List<String> foo() {
        MyContinuation continuation = new MyContinuation();
        DefaultParametersInSuspsendWithJvmOverloadsKt.suspendHere(continuation);
        DefaultParametersInSuspsendWithJvmOverloadsKt.suspendHere("cde", continuation);
        DefaultParametersInSuspsendWithJvmOverloadsKt.suspendHere("xyz", 9, continuation);
        return result;
    }

    private class MyContinuation implements Continuation<String> {
        @Override
        public CoroutineContext getContext() {
            return EmptyCoroutineContext.INSTANCE;
        }

        @Override
        public void resumeWith(Object x) {
            result.add((String) x);
        }
    }
}
