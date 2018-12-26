// "Convert expression to 'Int'" "true"
// WITH_RUNTIME
fun foo(param: Int) {}

fun test(expr: UInt) {
    foo(<caret>expr)
}