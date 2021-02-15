open class A
class B
class C: A()

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