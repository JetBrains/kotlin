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

    <!NI;OVERLOAD_RESOLUTION_AMBIGUITY, OI;NONE_APPLICABLE!>take<!>(B::<!NI;DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>)
}
