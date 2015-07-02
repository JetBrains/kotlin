// WITH_RUNTIME
// IS_APPLICABLE: FALSE
fun String.withIndices(): Int = 42

fun foo(s: String) {
    for (<caret>a in s) {

    }
}