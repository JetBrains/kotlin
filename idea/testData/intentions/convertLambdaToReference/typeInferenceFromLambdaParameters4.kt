fun overloadFun(p: Int, q: Long) {}

fun <T, U> foo(fn: (T, U) -> Unit) {}

fun test() {
    foo {<caret> x: Int, y: Long -> overloadFun(x, y) }
}