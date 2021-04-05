// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

abstract class SubFunction : kotlin.Function0<Unit>

fun <T> takeIt(x: T, f: SubFunction) {}

fun cr() {}

fun test() {
    <!INAPPLICABLE_CANDIDATE!>takeIt<!>(42, ::<!UNRESOLVED_REFERENCE!>cr<!>)
    takeIt(42, <!ARGUMENT_TYPE_MISMATCH!>{ }<!>)
}
