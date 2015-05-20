fun foo(i: Int) {}
fun foo(a: IntArray) {}
fun foo(a: String, b: Int) {}
fun foo() {}

fun test() {
    foo(bar())
}