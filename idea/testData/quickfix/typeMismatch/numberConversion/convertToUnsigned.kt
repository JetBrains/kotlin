// "Convert expression to 'UInt'" "true"
// WITH_RUNTIME
fun foo(param: UInt) {}

fun test(expr: Int) {
    foo(<caret>expr)
}