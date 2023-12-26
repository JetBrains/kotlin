// !OPT_IN: kotlin.contracts.ExperimentalContracts
// WITH_STDLIB

// JVM_ABI_K1_K2_DIFF: KT-62464, KT-63971

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


data class Content<out T>(val value: T)

fun <T> content(value: T) = Content(value)

@ExperimentalContracts
inline fun <R, T : R> Content<T>.getOrElse(
    onException: (exception: Exception) -> R,
): R = fold({ it }, onException)

@ExperimentalContracts
inline fun <R, T> Content<T>.fold(
    onContent: (value: T) -> R,
    onException: (exception: Exception) -> R,
): R {
    contract {
        callsInPlace(onContent, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onException, InvocationKind.AT_MOST_ONCE)
    }
    return onContent(value)
}


@ExperimentalContracts
fun box(): String {
    val t = content(1).getOrElse { 2 }
    if (t != 1) return "Failed: $t"

    return "OK"
}
