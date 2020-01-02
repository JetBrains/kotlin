// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

class A<T : CharSequence>(x: T)

fun <E : CharSequence> foo1(x: E) {}
fun <E : CharSequence> E.foo2() {}

fun <F : String?> bar(x: F) {
    <!INAPPLICABLE_CANDIDATE!>A<!>(x)
    <!INAPPLICABLE_CANDIDATE!>A<!><F>(x)

    <!INAPPLICABLE_CANDIDATE!>foo1<!>(x)
    <!INAPPLICABLE_CANDIDATE!>foo1<!><F>(x)

    x.<!INAPPLICABLE_CANDIDATE!>foo2<!>()
    x.<!INAPPLICABLE_CANDIDATE!>foo2<!><F>()
}

