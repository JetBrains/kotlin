// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY
// !CHECK_TYPE
open class Outer<X, Y> {
    inner class Inner<Z>
    typealias Alias<W> = Map<W, X>
}

open class BaseDerived1<E, F> : Outer<F, E>()
open class BaseDerived2<X> : BaseDerived1<String, X>()

class Derived : BaseDerived2<Int>() {
    fun foo(): Inner<Char> = null!!
    fun baz(): Alias<Char> = null!!
}

fun foo() {
    Derived().foo() checkType { _<Outer<Int, String>.Inner<Char>>() }
    Derived().baz() checkType { _<Map<Char, Int>>() }
}
