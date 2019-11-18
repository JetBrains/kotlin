// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// CHECK_STATE_MACHINE

// FILE: inlineMe.kt

package test

interface SuspendRunnable {
    suspend fun run()
}

inline fun inlineMe(crossinline c: suspend () -> Unit) = object : SuspendRunnable {
    override suspend fun run() {
        c(); c()
    }
}

inline fun inlineMe2(crossinline c: suspend () -> Unit) = inlineMe { c(); c() }

inline fun inlineMe3(crossinline c: suspend () -> Unit) = object: SuspendRunnable {
    override suspend fun run() {
        var sr = inlineMe {
            c()
            c()
        }
        sr.run()
        sr = inlineMe {
            c()
            c()
        }
        sr.run()
    }
}

// FILE: A.java

import test.InlineMeKt;
import helpers.CoroutineUtilKt;
import helpers.EmptyContinuation;
import kotlin.jvm.functions.Function1;
import COROUTINES_PACKAGE.Continuation;
import kotlin.Unit;

public class A {
    static Function1<Continuation<? super Unit>, Object> callback = new Function1<Continuation<? super Unit>, Object>() {
        @Override
        public Object invoke(Continuation<? super Unit> continuation) {
            return CoroutineUtilKt.getStateMachineChecker().suspendHere(continuation);
        }
    };
    public static Object call() {
        return InlineMeKt.inlineMe(callback);
    }
    public static Object call2() {
        return InlineMeKt.inlineMe2(callback);
    }
    public static Object call3() {
        return InlineMeKt.inlineMe3(callback);
    }
}

// FILE: box.kt

import test.*
import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(CheckStateMachineContinuation)
}

fun box(): String {
    builder {
        (A.call() as SuspendRunnable).run()
    }
    StateMachineChecker.check(2)
    StateMachineChecker.reset()

    builder {
        (A.call2() as SuspendRunnable).run()
    }
    StateMachineChecker.check(4)
    StateMachineChecker.reset()

    builder {
        (A.call3() as SuspendRunnable).run()
    }
    StateMachineChecker.check(8)
    StateMachineChecker.reset()

    builder {
        inlineMe {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }.run()
    }
    StateMachineChecker.check(4)
    StateMachineChecker.reset()

    builder {
        inlineMe2 {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }.run()
    }
    StateMachineChecker.check(8)
    StateMachineChecker.reset()

    builder {
        inlineMe3 {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }.run()
    }
    StateMachineChecker.check(16)
    return "OK"
}