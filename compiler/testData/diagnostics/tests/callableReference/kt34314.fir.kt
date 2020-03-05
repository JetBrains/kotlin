// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE

fun main() {
    val x = run { ::run } // no error
}
