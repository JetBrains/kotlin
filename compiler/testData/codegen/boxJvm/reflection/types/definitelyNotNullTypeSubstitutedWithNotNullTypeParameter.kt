// TARGET_BACKEND: JVM
// WITH_REFLECT

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
    B::class.check("fun B<U>.foo(U): U", "fun B<U>.bar(kotlin.collections.List<U>): kotlin.Unit")
    C::class.check("fun C<V, W>.foo(W): W", "fun C<V, W>.bar(kotlin.collections.List<W>): kotlin.Unit")

    D::class.check("fun D<X>.foo(X & Any): X & Any", "fun D<X>.bar(kotlin.collections.List<X & Any>): kotlin.Unit")
    E::class.check("fun E<Y, Z>.foo(Z & Any): Z & Any", "fun E<Y, Z>.bar(kotlin.collections.List<Z & Any>): kotlin.Unit")

    return "OK"
}
