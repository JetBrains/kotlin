package test

private fun foo(p: Int) {}
fun foo(lessSpecific: Any) {}

fun test() {
    f<caret>oo(42)
}