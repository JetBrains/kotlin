// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun foo(y: Boolean) {
    val <!UNUSED_VARIABLE!>x<!>: Int = 42
    <!CONTRACT_NOT_ALLOWED!>contract {
        returns() implies y
    }<!>
}