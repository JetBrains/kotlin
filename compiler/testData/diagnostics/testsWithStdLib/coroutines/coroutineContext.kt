// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.coroutines.experimental.intrinsics.coroutineContext
import kotlin.coroutines.experimental.CoroutineContext

fun ordinal() {
    <!ILLEGAL_SUSPEND_PROPERTY_ACCESS!>coroutineContext<!>
}

suspend fun named() {
    coroutineContext
}

class A {
    val coroutineContext = kotlin.coroutines.experimental.intrinsics.<!ILLEGAL_SUSPEND_PROPERTY_ACCESS!>coroutineContext<!>
}

class Controller {
    fun ordinal() {
        <!ILLEGAL_SUSPEND_PROPERTY_ACCESS!>coroutineContext<!>
    }

    suspend fun named() {
        coroutineContext
    }

    suspend fun severalArgs(s: String, a: Any) {
        coroutineContext
    }
}

fun builder(c: () -> CoroutineContext) = {}
fun builderSuspend(c: suspend () -> CoroutineContext) = {}

fun builderSeveralArgs(c: (Int, Int, Int) -> CoroutineContext) = {}
fun builderSuspendSeveralArgs(c: suspend (Int, Int, Int) -> CoroutineContext) = {}

fun test() {
    builder { <!ILLEGAL_SUSPEND_PROPERTY_ACCESS!>coroutineContext<!> }
    builderSuspend { coroutineContext }
    builderSeveralArgs {_, _,_ -> <!ILLEGAL_SUSPEND_PROPERTY_ACCESS!>coroutineContext<!> }
    builderSuspendSeveralArgs {_, _,_ -> coroutineContext}
}
