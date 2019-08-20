// IGNORE_BACKEND: JS_IR, JS, NATIVE
// WITH_REFLECT

open class A {
    open fun foo(a: String, b: String = "b") = b + a
}

class B : A() {
    override fun foo(a: String, b: String) = a + b
}

fun box(): String {
    val f = B::foo

    assert("ab" == f.callBy(mapOf(
        f.parameters.first() to B(),
        f.parameters.single { it.name == "a" } to "a"
    )))

    return "OK"
}
