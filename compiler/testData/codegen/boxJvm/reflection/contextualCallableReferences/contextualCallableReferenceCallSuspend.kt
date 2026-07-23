// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// WITH_COROUTINES
// ISSUE: KT-86452

import helpers.*
import kotlin.coroutines.startCoroutine
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.callSuspendBy

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

context(c: String)
suspend fun suspendFun(suffix: String): String = c + suffix

context(c: String)
suspend fun withDefault(x: Int = 7): String = "$c-$x"

class Cls {
    context(c: String)
    suspend fun member(): String = "$c-member"
}

var result = "FAIL: not run"

fun box(): String {
    context("ctx") {
        val sf = ::suspendFun
        val wd = ::withDefault
        val m = Cls::member

        if (!sf.isSuspend) return "FAIL 0: reference to a contextual suspend function is not marked isSuspend"

        builder {
            // fully bound context argument, one unbound value parameter
            val r1 = sf.callSuspend("K")
            if (r1 != "ctxK") { result = "FAIL 1: $r1"; return@builder }

            // callSuspendBy through the $default method: the default-argument mask must account for the bound context argument
            val r2 = wd.callSuspendBy(emptyMap())
            if (r2 != "ctx-7") { result = "FAIL 2: $r2"; return@builder }

            val r3 = wd.callSuspendBy(mapOf(wd.parameters[0] to 5))
            if (r3 != "ctx-5") { result = "FAIL 3: $r3"; return@builder }

            // bound context argument, UNBOUND dispatch receiver passed to callSuspend
            val r4 = m.callSuspend(Cls())
            if (r4 != "ctx-member") { result = "FAIL 4: $r4"; return@builder }

            result = "OK"
        }
    }
    return result
}
