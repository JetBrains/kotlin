fun overloadFun(p: Int, q: Long) {}
fun overloadFun(p: String, q: Long) {}

fun <T, U> foo(fn: (T, U) -> Unit) {}

fun test() {
    foo {<caret> x: String, y: Long -> overloadFun(x, y) }
}