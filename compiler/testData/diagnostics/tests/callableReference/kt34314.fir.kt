// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE

fun main() {
    val x = <!NEW_INFERENCE_ERROR!>run { ::run }<!> // no error
}
