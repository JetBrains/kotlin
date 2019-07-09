// "Change to '1u'" "true"
// WITH_RUNTIME
fun foo(param: UInt) {}

fun test() {
    foo(<caret>1)
}