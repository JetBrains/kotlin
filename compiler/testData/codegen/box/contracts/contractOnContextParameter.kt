// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY
// SKIP_IR_DESERIALIZATION_CHECKS
// ^ Context parameter names aren't serialized to metadata yet KT-74546
// MODULE: a
// FILE: a.kt
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
context(a: String?)
fun validate1() {
    contract {
        returns() implies (a != null)
    }
    a!!
}

// MODULE: b(a)
// FILE: b.kt
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
context(a: String?)
fun validate2() {
    contract {
        returns() implies (a != null)
    }
    a!!
}

fun box(): String {
    return with("O" as String?) {
        validate1()
        this
    } + with("K" as String?) {
        validate2()
        this
    }
}