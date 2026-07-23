// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

context(c: String)
suspend fun suspendFun(suffix: String): String = c + suffix

context(c: String)
fun plainFun(suffix: String): String = c + suffix

var result = "FAIL: not run"

fun box(): String {
    context("O") {
        // reference to a contextual *suspend* function
        val s: suspend (String) -> String = ::suspendFun
        // suspend-conversion adapter over a contextual non-suspend function
        val converted: suspend (String) -> String = ::plainFun

        builder {
            val r1 = s("K")
            if (r1 != "OK") {
                result = "FAIL 1: $r1"
                return@builder
            }
            val r2 = converted("K")
            if (r2 != "OK") {
                result = "FAIL 2: $r2"
                return@builder
            }
            result = "OK"
        }
    }
    return result
}
