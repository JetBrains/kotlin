package rendererTest

class C<T: C<T>> {
    fun foo(c: C<*>) {}
}

fun foo(c: C<*>) {}
fun foo1(c: C<C<*>>) {}

//package rendererTest
//internal final class C<T : rendererTest.C<T>> defined in rendererTest
//public constructor C<T : rendererTest.C<T>>() defined in rendererTest.C
//<T : rendererTest.C<T>> defined in rendererTest.C
//internal final fun foo(c: rendererTest.C<*>): kotlin.Unit defined in rendererTest.C
//value-parameter val c: rendererTest.C<*> defined in rendererTest.C.foo
//internal fun foo(c: rendererTest.C<*>): kotlin.Unit defined in rendererTest
//value-parameter val c: rendererTest.C<*> defined in rendererTest.foo
//internal fun foo1(c: rendererTest.C<rendererTest.C<*>>): kotlin.Unit defined in rendererTest
//value-parameter val c: rendererTest.C<rendererTest.C<*>> defined in rendererTest.foo1