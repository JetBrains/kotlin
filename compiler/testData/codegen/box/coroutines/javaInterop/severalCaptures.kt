// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// CHECK_STATE_MACHINE

// FILE: inlineMe.kt

package test

import helpers.*

interface SuspendRunnable {
    suspend fun run1()
    suspend fun run2()
}

inline fun inlineMe(crossinline c1: suspend () -> Unit, crossinline c2: suspend () -> Unit) = object : SuspendRunnable {
    override suspend fun run1() {
        c1(); c1()
    }
    override suspend fun run2() {
        c2(); c2()
    }
}

inline fun inlineMe2(crossinline c1: suspend () -> Unit, crossinline c2: suspend () -> Unit) = inlineMe({ c1(); c1() }) { c2(); c2() }

inline fun inlineMe3(crossinline c1: suspend () -> Unit, crossinline c2: suspend () -> Unit) = object : SuspendRunnable {
    override suspend fun run1() {
        val sr = inlineMe({ c1(); c1() }) { c2(); c2() }
        sr.run1()
        sr.run2()
    }
    override suspend fun run2() {
        val sr = inlineMe2({ c1(); c1() }) { c2(); c2() }
        sr.run1()
        sr.run2()
    }
}

inline fun inlineMe4(crossinline c1: suspend () -> Unit, crossinline c2: suspend () -> Unit) = object : SuspendRunnable {
    override suspend fun run1() {
        val sr = suspend {
            c1();
            c2()
        }
        sr()
        sr()
    }
    override suspend fun run2() {
        val sr = object : SuspendRunnable {
            override suspend fun run1() {
                c1(); c1()
            }
            override suspend fun run2() {
                c2(); c2()
            }
        }
        sr.run1()
        sr.run2()
    }
}

inline fun inlineMe5(crossinline c1: suspend () -> Unit) = inlineMe({ c1(); c1() }) {
    StateMachineChecker.suspendHere()
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
        return InlineMeKt.inlineMe(callback, callback);
    }
    public static Object call2() {
        return InlineMeKt.inlineMe2(callback, callback);
    }
    public static Object call3() {
        return InlineMeKt.inlineMe3(callback, callback);
    }
    public static Object call4() {
        return InlineMeKt.inlineMe4(callback, callback);
    }
    public static Object call5() {
        return InlineMeKt.inlineMe5(callback);
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
        (A.call() as SuspendRunnable).run1()
    }
    StateMachineChecker.check(2)
    StateMachineChecker.reset()

    builder {
        (A.call() as SuspendRunnable).run2()
    }
    StateMachineChecker.check(2)
    StateMachineChecker.reset()

    builder {
        (A.call2() as SuspendRunnable).run1()
    }
    StateMachineChecker.check(4)
    StateMachineChecker.reset()

    builder {
        (A.call2() as SuspendRunnable).run2()
    }
    StateMachineChecker.check(4)
    StateMachineChecker.reset()

    builder {
        (A.call3() as SuspendRunnable).run1()
    }
    StateMachineChecker.check(8)
    StateMachineChecker.reset()

    builder {
        (A.call3() as SuspendRunnable).run2()
    }
    StateMachineChecker.check(16)
    StateMachineChecker.reset()

    builder {
        (A.call4() as SuspendRunnable).run1()
    }
    StateMachineChecker.check(4)
    StateMachineChecker.reset()

    builder {
        (A.call4() as SuspendRunnable).run2()
    }
    StateMachineChecker.check(4)
    StateMachineChecker.reset()

    builder {
        (A.call5() as SuspendRunnable).run1()
    }
    StateMachineChecker.check(4)
    StateMachineChecker.reset()

    builder {
        (A.call5() as SuspendRunnable).run2()
    }
    StateMachineChecker.check(2)
    StateMachineChecker.reset()

    builder {
        inlineMe({
                     StateMachineChecker.suspendHere()
                     StateMachineChecker.suspendHere()
                 }) {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }.run1()
    }
    StateMachineChecker.check(4)
    StateMachineChecker.reset()
    builder {
        inlineMe({
                     StateMachineChecker.suspendHere()
                     StateMachineChecker.suspendHere()
                 }) {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }.run2()
    }
    StateMachineChecker.check(4)
    StateMachineChecker.reset()

    builder {
        inlineMe2 ({
                       StateMachineChecker.suspendHere()
                       StateMachineChecker.suspendHere()
                   }) {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }.run1()
    }
    StateMachineChecker.check(8)
    StateMachineChecker.reset()
    builder {
        inlineMe2 ({
                       StateMachineChecker.suspendHere()
                       StateMachineChecker.suspendHere()
                   }) {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }.run2()
    }
    StateMachineChecker.check(8)
    StateMachineChecker.reset()

    builder {
        inlineMe3 ({
                       StateMachineChecker.suspendHere()
                       StateMachineChecker.suspendHere()
                   }) {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }.run1()
    }
    StateMachineChecker.check(16)
    StateMachineChecker.reset()

    builder {
        inlineMe3 ({
                       StateMachineChecker.suspendHere()
                       StateMachineChecker.suspendHere()
                   }) {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }.run2()
    }
    StateMachineChecker.check(32)
    StateMachineChecker.reset()

    builder {
        inlineMe4 ({
                       StateMachineChecker.suspendHere()
                       StateMachineChecker.suspendHere()
                   }) {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }.run1()
    }
    StateMachineChecker.check(8)
    StateMachineChecker.reset()

    builder {
        inlineMe4 ({
                       StateMachineChecker.suspendHere()
                       StateMachineChecker.suspendHere()
                   }) {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }.run2()
    }
    StateMachineChecker.check(8)
    StateMachineChecker.reset()

    builder {
        inlineMe5 {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }.run1()
    }
    StateMachineChecker.check(8)
    StateMachineChecker.reset()

    builder {
        inlineMe5 {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }.run2()
    }
    StateMachineChecker.check(2)
    StateMachineChecker.reset()

    return "OK"
}