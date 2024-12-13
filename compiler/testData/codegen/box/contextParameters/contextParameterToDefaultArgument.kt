// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
class C(var a: String) {
    fun foo(): String { return a }
}

context(a: C)
fun test(b: C = a): String {
    return b.foo()
}

fun box(): String {
    with(C("OK")) {
        return test()
    }
}