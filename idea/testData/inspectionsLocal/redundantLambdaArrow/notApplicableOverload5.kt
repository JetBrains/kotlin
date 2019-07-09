// WITH_RUNTIME
// PROBLEM: none

fun bar(f: () -> Unit) {}
fun bar(f: (Int) -> Unit) {}

fun test() {
    bar { it<caret> -> }
}