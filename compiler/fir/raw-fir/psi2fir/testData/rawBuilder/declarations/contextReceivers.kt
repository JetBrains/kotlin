context(A, b@B)
fun foo() {}

context(A, b@B)
val x: Int get() = 1

context(A, b@B)
class C

fun bar1(x: context(A, B)() -> Unit) {}
fun bar2(x: context(A, B) C.() -> Unit) {}
