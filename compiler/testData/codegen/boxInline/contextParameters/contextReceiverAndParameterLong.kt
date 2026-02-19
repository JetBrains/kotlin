// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND_MULTI_MODULE: JVM_IR, JVM_IR_SERIALIZE
// LANGUAGE: +ContextParameters
// NO_CHECK_LAMBDA_INLINING

// FILE: 1.kt

@OptIn(kotlin.contracts.ExperimentalContracts::class)
public inline fun <T, T1, T2, R> context(with: T, receiver: T1, param: T2, block: context(T) T1.(T2) -> R): R {
    kotlin.contracts.contract {
        callsInPlace(block, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return block(with, receiver, param)
}

context(context: A)
public inline fun <A> contextOf(): A = context

// FILE: 2.kt

var result = 0L

fun contextOfWithContextParameter() {
    fun <A> withLong(block: context(Long) Double.(Long) -> A): A =
        context(1L, 10.0, 100L) { block(it) }

    withLong {
        result = contextOf<Long>() + this@withLong.toLong() + it
    }
}

fun box(): String {
    contextOfWithContextParameter()
    if (result == 111L) return "OK"
    return "$result"
}
