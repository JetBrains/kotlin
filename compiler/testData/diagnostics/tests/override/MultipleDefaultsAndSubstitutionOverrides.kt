// FIR_IDENTICAL
interface A<T> {
    fun foo(x: Int = 42): Int
}

open class B<K> {
    fun foo(x: Int = 239) = x
}

interface C<E> {
    fun foo(x: Int): Int
}

open <!MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE!>class Z<!><R> : A<R>, B<R>(), C<R>

class N<W> : Z<W>()
