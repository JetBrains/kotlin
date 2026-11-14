// TARGET_BACKEND: JVM
// WITH_REFLECT
package test

import kotlin.reflect.KClass
import kotlin.test.assertEquals

open class A<T> {
    fun foo(x: T & Any): T & Any = x
    fun bar(x: List<T & Any>) {}
}

class B<U : Any> : A<U>()

class C<V : CharSequence, W : V> : A<W>()

class D<X> : A<X>()

class E<Y : CharSequence?, Z : Y> : A<Z>()

private fun KClass<*>.check(expectedFoo: String, expectedBar: String) {
    assertEquals(expectedFoo, members.single { it.name == "foo" }.toString())
    assertEquals(expectedBar, members.single { it.name == "bar" }.toString())
}

fun box(): String {
    B::class.check("fun test.B<U>.foo(U): U", "fun test.B<U>.bar(kotlin.collections.List<U>): kotlin.Unit")
    C::class.check("fun test.C<V, W>.foo(W): W", "fun test.C<V, W>.bar(kotlin.collections.List<W>): kotlin.Unit")

    D::class.check("fun test.D<X>.foo(X & Any): X & Any", "fun test.D<X>.bar(kotlin.collections.List<X & Any>): kotlin.Unit")
    E::class.check("fun test.E<Y, Z>.foo(Z & Any): Z & Any", "fun test.E<Y, Z>.bar(kotlin.collections.List<Z & Any>): kotlin.Unit")

    return "OK"
}
