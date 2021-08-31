// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER -TOPLEVEL_TYPEALIASES_ONLY

class A<R1, R2, R3, R4>

private fun <E> foobar() = {
    open class LocalOuter<X, Y> {
        inner class LocalInner<Z> {
            fun a() = A<E, X, Y, Z>()
        }

        typealias LocalAlias<W> = A<E, <!UNRESOLVED_REFERENCE!>X<!>, <!UNRESOLVED_REFERENCE!>Y<!>, W>
    }

    class Derived : LocalOuter<Double, Short>() {
        fun foo(): LocalInner<Long> = null!!
        fun bar(): <!UNRESOLVED_REFERENCE!>LocalAlias<Char><!> = null!!
    }

    Derived()
}

private fun noParameters() = {
    open class LocalOuter2<X, Y> {
        inner class LocalInner2<Z> {
            fun a() = A<Any, X, Y, Z>()
        }

        typealias LocalAlias2<W> = A<Any, <!UNRESOLVED_REFERENCE!>X<!>, <!UNRESOLVED_REFERENCE!>Y<!>, W>
    }

    class Derived2 : LocalOuter2<Double, Short>() {
        fun foo(): LocalInner2<Long> = null!!
        fun bar(): <!UNRESOLVED_REFERENCE!>LocalAlias2<Char><!> = null!!
    }

    Derived2()
}

fun test() {
    var x = foobar<String>()
    x = foobar<String>()

    x().foo().a() checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><A<String, Double, Short, Long>>() }
    x().bar() <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>checkType<!> { _<A<String, Double, Short, Char>>() }

    x = foobar<Int>()

    var y = noParameters()
    y = noParameters()

    y().foo().a() checkType { _<A<Any, Double, Short, Long>>() }
    y().bar() <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>checkType<!> { _<A<Any, Double, Short, Char>>() }
}
