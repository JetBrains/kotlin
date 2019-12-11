// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE

class A<T> {
    fun foo(f: (T) -> Unit) {}
}

fun test(a: A<out Number>, b: A<in Number>) {
    a.foo {
        it checkType { _<Number>() }
    }

    b.foo {
        it checkType { <!INAPPLICABLE_CANDIDATE!>_<!><Any?>() }
    }
}
