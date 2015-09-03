// !DIAGNOSTICS: -UNUSED_PARAMETER

class A<T : CharSequence>(x: T)

fun <E : CharSequence> foo1(x: E) {}
fun <E : CharSequence> E.foo2() {}

fun <F : String?> bar(x: F) {
    <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>A<!>(x)
    A<<!UPPER_BOUND_VIOLATED!>F<!>>(x)

    <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>foo1<!>(x)
    foo1<<!UPPER_BOUND_VIOLATED!>F<!>>(x)

    x.<!TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>foo2<!>()
    x.foo2<<!UPPER_BOUND_VIOLATED!>F<!>>()
}

