// "Wrap with '?.let { ... }' call" "true"
// WITH_RUNTIME
class A(val b: B)
class B {
    fun c(s: String) {}
}

class X(val y: Y)
class Y(val z: String)

fun test(a: A, x: X?) {
    a.b.c(x?.y<caret>.z)
}