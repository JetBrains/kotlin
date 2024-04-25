// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect +AllowReifiedGenericsInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER -UNUSED_VARIABLE

import kotlin.contracts.*

inline fun <reified T> requireIsInstance(value: Any?) {
    contract {
        returns() implies (value is T)
    }
    if (value !is T) {
        throw IllegalArgumentException()
    }
}

inline fun <reified T> cast(value: Any?): T {
    contract {
        returns() implies (value is T)
    }
    return value as T
}

fun test_1(x: Any) {
    requireIsInstance<String>(x)
    x.length
}

fun test_2(x: Any) {
    val s: String = cast(x)
    x.length
}
