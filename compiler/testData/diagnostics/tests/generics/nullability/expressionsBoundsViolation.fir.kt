// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

class A<T : CharSequence>(x: T)

fun <E : CharSequence> foo1(x: E) {}
fun <E : CharSequence> E.foo2() {}

fun <F : String?> bar(x: F) {
    A(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
    A<F>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)

    foo1(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
    foo1<F>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)

    x.<!INAPPLICABLE_CANDIDATE!>foo2<!>()
    x.<!INAPPLICABLE_CANDIDATE!>foo2<!><F>()
}

