// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.*

interface A
interface B : A

fun A.foo() {}

fun take(f: (A) -> Unit) {}
fun take(f: () -> Unit) {}

fun test() {
    B::foo checkType { _<KFunction1<B, Unit>>() }

    <!NONE_APPLICABLE!>take<!>(B::foo)
}
