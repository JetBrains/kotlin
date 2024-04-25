// DIAGNOSTICS: -UNUSED_PARAMETER

class A<T : CharSequence>(x: T)

fun <E : CharSequence> foo1(x: E) {}
fun <E : CharSequence> E.foo2() {}

fun <F : String?> bar(x: F) {
    A(<!TYPE_MISMATCH!>x<!>)
    A<<!UPPER_BOUND_VIOLATED!>F<!>>(x)

    foo1(<!TYPE_MISMATCH!>x<!>)
    foo1<<!UPPER_BOUND_VIOLATED!>F<!>>(x)

    x<!UNSAFE_CALL!>.<!>foo2()
    x.foo2<<!UPPER_BOUND_VIOLATED!>F<!>>()
}
