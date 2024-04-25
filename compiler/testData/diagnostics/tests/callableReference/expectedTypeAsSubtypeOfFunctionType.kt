// DIAGNOSTICS: -UNUSED_PARAMETER

abstract class SubFunction : kotlin.Function0<Unit>

fun <T> takeIt(x: T, f: SubFunction) {}

fun cr() {}

fun test() {
    takeIt(42, <!TYPE_MISMATCH!>::cr<!>)
    takeIt(42, <!TYPE_MISMATCH!>{ }<!>)
}
