// IGNORE_BACKEND: JVM_IR, JS_IR, JS, NATIVE
// WITH_REFLECT

import kotlin.test.assertEquals

open class A<T> {
    fun foo(t: T) {}
}

open class B<U> : A<U>()

class C : B<String>()

fun box(): String {
    assertEquals("fun A<T>.foo(T): kotlin.Unit", A<Double>::foo.toString())
    assertEquals("fun B<U>.foo(U): kotlin.Unit", B<Float>::foo.toString())
    assertEquals("fun C.foo(kotlin.String): kotlin.Unit", C::foo.toString())

    val afoo = A::class.members.single { it.name == "foo" }
    assertEquals("fun A<T>.foo(T): kotlin.Unit", afoo.toString())
    val bfoo = B::class.members.single { it.name == "foo" }
    assertEquals("fun B<U>.foo(U): kotlin.Unit", bfoo.toString())
    val cfoo = C::class.members.single { it.name == "foo" }
    assertEquals("fun C.foo(kotlin.String): kotlin.Unit", cfoo.toString())

    return "OK"
}
