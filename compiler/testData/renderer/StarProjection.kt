package rendererTest

class C<T: C<T>> {
    fun foo(c: C<*>) {}
}

fun foo(c: C<*>) {}
fun foo1(c: C<C<*>>) {}

//package rendererTest
//public final class C<T : rendererTest.C<T>> defined in rendererTest
//public constructor C<T : rendererTest.C<T>>() defined in rendererTest.C
//<T : rendererTest.C<T>> defined in rendererTest.C
//public final fun foo(c: rendererTest.C<*>): kotlin.Unit defined in rendererTest.C
//value-parameter c: rendererTest.C<*> defined in rendererTest.C.foo
//public fun foo(c: rendererTest.C<*>): kotlin.Unit defined in rendererTest
//value-parameter c: rendererTest.C<*> defined in rendererTest.foo
//public fun foo1(c: rendererTest.C<rendererTest.C<*>>): kotlin.Unit defined in rendererTest
//value-parameter c: rendererTest.C<rendererTest.C<*>> defined in rendererTest.foo1