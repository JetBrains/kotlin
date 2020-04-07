// !WITH_NEW_INFERENCE
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

    <!INAPPLICABLE_CANDIDATE!>take<!>(B::foo)
}
