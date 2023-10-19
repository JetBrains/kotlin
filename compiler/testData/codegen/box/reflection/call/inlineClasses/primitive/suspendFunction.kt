// TARGET_BACKEND: JVM
// WITH_REFLECT
// WITH_COROUTINES

import kotlin.coroutines.startCoroutine
import kotlin.reflect.full.callSuspend
import kotlin.test.assertEquals
import helpers.*

inline class Z(val value: Int)

class C {
    private var value: Z = Z(0)

    suspend fun nonNullConsume(z: Z) { value = z }
    suspend fun nonNullProduce(): Z = value
    suspend fun nullableConsume(z: Z?) { value = z!! }
    suspend fun nullableProduce(): Z? = value
    suspend fun nonNull_nonNullConsumeAndProduce(z: Z): Z = z
    suspend fun nonNull_nullableConsumeAndProduce(z: Z): Z? = z
    suspend fun nullable_nonNullConsumeAndProduce(z: Z?): Z = z!!
    suspend fun nullable_nullableConsumeAndProduce(z: Z?): Z? = z
}

private fun run0(f: suspend () -> Int): Int {
    var result = -1
    f.startCoroutine(handleResultContinuation { result = it })
    return result
}

fun box(): String {
    val c = C()

    run0 {
        C::nonNullConsume.callSuspend(c, Z(1))
        C::nonNullProduce.callSuspend(c).value
    }.let { assertEquals(1, it) }

    run0 {
        C::nullableConsume.callSuspend(c, Z(2))
        C::nullableProduce.callSuspend(c)!!.value
    }.let { assertEquals(2, it) }

    run0 {
        C::nonNull_nonNullConsumeAndProduce.callSuspend(c, Z(3)).value
    }.let { assertEquals(3, it) }

    run0 {
        C::nonNull_nullableConsumeAndProduce.callSuspend(c, Z(4))!!.value
    }.let { assertEquals(4, it) }

    run0 {
        C::nullable_nonNullConsumeAndProduce.callSuspend(c, Z(5)).value
    }.let { assertEquals(5, it) }

    run0 {
        C::nullable_nullableConsumeAndProduce.callSuspend(c, Z(6))!!.value
    }.let { assertEquals(6, it) }

    return "OK"
}
