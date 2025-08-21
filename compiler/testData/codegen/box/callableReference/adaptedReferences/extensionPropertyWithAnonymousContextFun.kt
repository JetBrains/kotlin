// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

class C(var a: String) {
    fun foo(): String {
        return a
    }
}

val C.y
    get() = context(x: C) fun (): String { return this@y.foo() + x.foo() }

fun foo(x: context(C) () -> String): String { return x(C("K")) }

fun box(): String {
    return foo(C::y.get(C("O")))
}