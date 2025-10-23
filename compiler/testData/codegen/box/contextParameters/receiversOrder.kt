// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY
class A(val x: String)

var result = ""

context(a: A)
fun foo() {
    result += a.x
}

fun A.bar() {
    foo()
    baz {
        foo()
    }
}

fun baz(a: A.() -> Unit) {
    a(A("K"))
}

fun box(): String {
    A("O").bar() // prints "1", "1" while "1", "2" is expected

    return result
}
