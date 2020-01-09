// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE

fun <T> materialize(): T = TODO()

fun main() {
    val x = run { materialize() }
}
