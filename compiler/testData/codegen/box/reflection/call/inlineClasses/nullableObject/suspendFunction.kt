// TARGET_BACKEND: JVM
// WITH_REFLECT
// WITH_COROUTINES

import kotlin.coroutines.startCoroutine
import kotlin.reflect.full.callSuspend
import kotlin.test.assertEquals
import helpers.*

@JvmInline
value class S(val value: String?)

class C {
    private var value: S = S("")

    suspend fun nonNullConsume(z: S) { value = z }
    suspend fun nonNullProduce(): S = value
    suspend fun nullableConsume(z: S?) { value = z!! }
    suspend fun nullableProduce(): S? = value
    suspend fun nonNull_nonNullConsumeAndProduce(z: S): S = z
    suspend fun nonNull_nullableConsumeAndProduce(z: S): S? = z
    suspend fun nullable_nonNullConsumeAndProduce(z: S?): S = z!!
    suspend fun nullable_nullableConsumeAndProduce(z: S?): S? = z
}

private fun run0(f: suspend () -> String): String {
    var result = ""
    f.startCoroutine(handleResultContinuation { result = it })
    return result
}

fun box(): String {
    val c = C()

    run0 {
        C::nonNullConsume.callSuspend(c, S("nonNull"))
        C::nonNullProduce.callSuspend(c).value!!
    }.let { assertEquals("nonNull", it) }

    run0 {
        C::nullableConsume.callSuspend(c, S("nullable"))
        C::nullableProduce.callSuspend(c)!!.value!!
    }.let { assertEquals("nullable", it) }

    run0 {
        C::nonNull_nonNullConsumeAndProduce.callSuspend(c, S("nonNull_nonNull")).value!!
    }.let { assertEquals("nonNull_nonNull", it) }

    run0 {
        C::nonNull_nullableConsumeAndProduce.callSuspend(c, S("nonNull_nullable"))!!.value!!
    }.let { assertEquals("nonNull_nullable", it) }

    run0 {
        C::nullable_nonNullConsumeAndProduce.callSuspend(c, S("nullable_nonNull")).value!!
    }.let { assertEquals("nullable_nonNull", it) }

    run0 {
        C::nullable_nullableConsumeAndProduce.callSuspend(c, S("nullable_nullable"))!!.value!!
    }.let { assertEquals("nullable_nullable", it) }

    return "OK"
}
