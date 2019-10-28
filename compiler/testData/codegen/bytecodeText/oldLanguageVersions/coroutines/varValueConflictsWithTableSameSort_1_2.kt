// !LANGUAGE: -ReleaseCoroutines
// IGNORE_BACKEND: JVM_IR
// WITH_COROUTINES

import helpers.*
// TREAT_AS_ONE_FILE
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*
suspend fun suspendHere(): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume("OK")
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = "fail 1"

    builder {
        // Initialize var with Int value
        try {
            var i: String = "abc"
            i = "123"
        } finally { }

        // This variable should take the same slot as 'i' had
        var s: String

        // We shout not spill 's' to continuation field because it's not effectively initialized
        if (suspendHere() == "OK") {
            s = "OK"
        }
        else {
            s = "fail 2"
        }

        result = s
    }

    return result
}

// 1 LOCALVARIABLE i Ljava/lang/String; L.* 3
// 1 LOCALVARIABLE s Ljava/lang/String; L.* 3
// 0 PUTFIELD VarValueConflictsWithTableSameSort_1_2Kt\$box\$1.L\$0 : Ljava/lang/Object;
/* 1 load in try/finally */
/* 1 load in result = s */
// 2 ALOAD 3
