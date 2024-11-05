// KT-72883: org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl cannot be cast to org.jetbrains.kotlin.ir.expressions.IrBlockBody
// IGNORE_BACKEND: JS_IR
// IGNORE_SYNTHETIC_ACCESSORS_CHECKS: JS_IR
// WITH_COROUTINES

// KT-72862: No function found for symbol
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE

// MODULE: lib
// FILE: a.kt
private suspend fun privateSuspendMethod() = "OK"

internal suspend inline fun internalInline() = privateSuspendMethod()

// MODULE: main()(lib)
// FILE: main.kt
import kotlin.coroutines.*

fun runBlocking(c: suspend () -> String): String {
    var res: String? = null
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        res = it.getOrThrow()
    })
    return res!!
}

fun box(): String = runBlocking {
    return@runBlocking internalInline()
}