// !DIAGNOSTICS: -UNUSED_PARAMETER

class A<T : CharSequence>(x: T)

fun <E : CharSequence> foo1(x: E) {}
fun <E : CharSequence> E.foo2() {}

fun <F : String?> bar(x: F) {
    A(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
    A<<!UPPER_BOUND_VIOLATED!>F<!>>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)

    foo1(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
    foo1<<!UPPER_BOUND_VIOLATED!>F<!>>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)

    x<!UNSAFE_CALL!>.<!>foo2()
    x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo2<!><F>()
}

