// !LANGUAGE: +ReturnsEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.internal.contracts.*

fun foo(y: Boolean) {
    val <!UNUSED_VARIABLE!>x<!>: Int = 42
    <!CONTRACT_NOT_ALLOWED!>contract {
        returns() implies y
    }<!>
}