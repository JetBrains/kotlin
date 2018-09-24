// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

class A<T : CharSequence>(x: T)

fun <E : CharSequence> foo1(x: E) {}
fun <E : CharSequence> E.foo2() {}

fun <F : String?> bar(x: F) {
    <!OI;TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>A<!>(<!NI;TYPE_MISMATCH!>x<!>)
    A<<!UPPER_BOUND_VIOLATED!>F<!>>(x)

    <!OI;TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>foo1<!>(<!NI;TYPE_MISMATCH!>x<!>)
    foo1<<!UPPER_BOUND_VIOLATED!>F<!>>(x)

    x<!NI;UNSAFE_CALL!>.<!><!OI;TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>foo2<!>()
    x.foo2<<!UPPER_BOUND_VIOLATED!>F<!>>()
}

