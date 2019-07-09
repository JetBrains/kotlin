// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

abstract class SubFunction : kotlin.Function0<Unit>

fun <T> takeIt(x: T, f: SubFunction) {}

fun cr() {}

fun test() {
    <!OI;TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR!>takeIt<!>(42, <!TYPE_MISMATCH!>::cr<!>)
    <!OI;TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR!>takeIt<!>(42, <!TYPE_MISMATCH!>{ }<!>)
}
