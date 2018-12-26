// "Change to '1'" "true"
// WITH_RUNTIME
fun foo(param: Int) {}

fun test() {
    foo(<caret>1u)
}