//!LANGUAGE: +DefinitelyNonNullableTypes

interface I<T> {
    fun input(t: T)
    fun output(): T
}

fun <T> foo(i: I<T & Any>) { i.input(i.output()) }

fun <T> bar(i: I<out T & Any>) = i.output()

fun <T> qux(t: T, i: I<in T & Any>) { i.input(t!!) }

class C<TT>(val t: TT): I<TT & Any> {
    override fun input(t: TT & Any) {}
    override fun output(): TT & Any = t!!
}

fun <T1 , T2 : I<T1 & Any>> foo2(p1: T1, p2: T2) { p2.input(p1!!) }
