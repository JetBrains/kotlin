// PROBLEM: none
// WITH_RUNTIME

fun bar(f: () -> Unit) {}
fun bar(f: (Int) -> Unit) {}

fun test() {
    bar({ -><caret> })
}