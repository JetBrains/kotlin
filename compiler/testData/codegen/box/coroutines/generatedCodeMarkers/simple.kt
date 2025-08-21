// WITH_STDLIB
// CHECK_BYTECODE_TEXT
// ENHANCED_COROUTINES_DEBUGGING

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun box(): String {
    var res = "FAIL"
    builder {
        res = simple()
    }
    return res
}

suspend fun dummy() {}

suspend fun simple(): String {
    dummy()
    dummy()
    return "OK"
}

// One suspend function with state-maching and one lambda
// 1 LOCALVARIABLE \$ecd\$checkContinuation\$38 I
// 1 LOCALVARIABLE \$ecd\$lambdaArgumentsUnspilling\$43 I
// 2 LOCALVARIABLE \$ecd\$tableswitch\$48 I
// 2 LOCALVARIABLE \$ecd\$checkResult\$53 I
// 2 LOCALVARIABLE \$ecd\$checkCOROUTINE_SUSPENDED\$58 I
// 2 LOCALVARIABLE \$ecd\$unreachable\$63 I
