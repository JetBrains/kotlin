// !DIAGNOSTICS: -UNUSED_PARAMETER

abstract class SubFunction : kotlin.Function0<Unit>

fun <T> takeIt(x: T, f: SubFunction) {}

fun cr() {}

fun test() {
    takeIt(42, ::<!UNRESOLVED_REFERENCE!>cr<!>)
    takeIt(42, <!ARGUMENT_TYPE_MISMATCH!>{ }<!>)
}
