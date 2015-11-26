fun ((T) -> G).foo<P> { }
fun ((T) -> G).foo { }
fun ((T) -> G).foo<P>
fun ((T) -> G).foo = 0
fun ((T) -> G)?.foo { }
fun ((T) -> G)??.foo { }

fun (@[a] T<T>.(A<B>, C<D, E>) -> ).foo() {}
fun fun @[a] T<T>.(A<B>).foo()

fun @[a] (T<T>.(A<B>)).foo()
fun @[a] ((A<B>)-).foo()

fun ((T)->G).foo<T>
class C<T>.

fun foo<c> {}
c<t>.

//-----------
class A<X> {
    fun <Y> foo() {
    }
}

fun bar(a: A<String>) {
    a.foo<Int>()
}