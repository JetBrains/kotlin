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
    B::foo checkType { <!UNRESOLVED_REFERENCE!>_<!><KFunction1<B, Unit>>() }

    take(B::foo)
}
