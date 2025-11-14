// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: 1.9.20
// ^^^KT-64148 is fixed in 2.0.0-Beta3

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
