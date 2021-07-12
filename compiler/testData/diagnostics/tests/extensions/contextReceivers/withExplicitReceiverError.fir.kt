class A
class B
class C

context(A)
fun B.f() {}

fun main() {
    val b = B()

    b.f()
    with(A()) {
        b.f()
    }
    with(C()) {
        b.f()
    }
}