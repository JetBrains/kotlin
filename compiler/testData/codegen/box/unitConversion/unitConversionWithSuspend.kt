// LANGUAGE: +UnitConversionsOnArbitraryExpressions
// ISSUE: KT-84393

import kotlin.coroutines.*

fun foo(f: suspend () -> Unit) {
    f.startCoroutine(Continuation(EmptyCoroutineContext) { it.getOrThrow() })
}

var sideEffect = ""

fun test(g: () -> String) {
    foo(g)
}

fun box(): String {
    test { sideEffect += "OK"; "ignored" }
    return sideEffect
}
