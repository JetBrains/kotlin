// TARGET_BACKEND: JVM
// WITH_STDLIB
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// CHECK_STATE_MACHINE

// FILE: inlineMe.kt

package test

inline suspend fun inlineMe(crossinline c: suspend () -> Unit) { c(); c() }

inline suspend fun inlineMe2(c: suspend () -> Unit) { c(); c() }

// FILE: A.java

import test.InlineMeKt;
import helpers.CoroutineUtilKt;
import helpers.EmptyContinuation;
import kotlin.jvm.functions.Function1;
import kotlin.coroutines.Continuation;
import kotlin.Unit;

public class A {
    static Function1<Continuation<? super Unit>, Object> callback = new Function1<Continuation<? super Unit>, Object>() {
        @Override
        public Object invoke(Continuation<? super Unit> continuation) {
            return CoroutineUtilKt.getStateMachineChecker().suspendHere(continuation);
        }
    };
    public static void call(Continuation<? super Unit> cont) {
        InlineMeKt.inlineMe(callback, cont);
    }
    public static void call2(Continuation<? super Unit> cont) {
        InlineMeKt.inlineMe2(callback, cont);
    }
}

// FILE: box.kt

import test.*
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(CheckStateMachineContinuation)
}

fun box(): String {
    A.call(CheckStateMachineContinuation)
    StateMachineChecker.check(2)
    StateMachineChecker.reset()

    builder {
        inlineMe {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }
    }
    StateMachineChecker.check(4)
    StateMachineChecker.reset()

    A.call2(CheckStateMachineContinuation)
    StateMachineChecker.check(2)

    StateMachineChecker.reset()
    builder {
        inlineMe2 {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }
    }
    StateMachineChecker.check(4)
    return "OK"
}
