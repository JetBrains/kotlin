// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_BACKEND_K1: JVM, JS_IR, JS_IR_ES6, WASM, NATIVE
// ^Reports FUN_INTERFACE_WITH_SUSPEND_FUNCTION on K1 JVM (non-IR) and fails in backend on others
// JVM_ABI_K1_K2_DIFF: KT-63877
// MODULE: m1
// FILE: m1.kt
typealias GivenLambda<ParentGivenType, GivenType> =
        suspend GivenDSL<ParentGivenType>.() -> GivenType

fun interface GivenDSL<ParentGivenType> {
    suspend fun given(): ParentGivenType
}

class GivenDSLHandler(
    val lambda: GivenLambda<*, *> = {},
) {
    suspend fun run(dsl: GivenDSL<*>): Any? {
        return dsl.lambda()
    }
}

// MODULE: m2(m1)
// FILE: box.kt
import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = "FAIL"

    builder {
        GivenDSLHandler(
            lambda = { result = this.given() as String; null },
        ).run { "OK" }
    }

    return result
}