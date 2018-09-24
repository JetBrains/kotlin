// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

open class A {
    fun foo() = "OK"
}

class B : A()

fun box(): String {
    val foo = B::class.members.single { it.name == "foo" }
    return foo.call(B()) as String
}
