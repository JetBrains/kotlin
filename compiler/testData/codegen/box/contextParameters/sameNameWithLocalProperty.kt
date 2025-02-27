// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

class A(val x: String) {
    fun foo(): String {
        return x
    }
}
var result = ""

context(a: A)
fun test1(): String {
    val a = A("O")
    return a.foo()
}

context(a: A)
fun test2(): String {
    val temp = a.foo()
    val a = A("not OK")
    return temp
}

fun box(): String {
    with(A("not OK")) {
        result += test1()
    }
    with(A("K")) {
        result += test2()
    }
    return result
}