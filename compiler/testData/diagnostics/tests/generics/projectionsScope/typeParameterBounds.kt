// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER

class Out<out X>
class In<in Y>
class Inv<Z>

class A<T> {
    fun <E : Out<T>> foo1(x: E) = 1
    fun <F : Inv<T>> foo2(x: F) = 1
    fun <G : In<T>>  foo3(x: G) = 1
}

fun foo2(a: A<out CharSequence>, b: A<in CharSequence>) {
    a.<!OI;TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>foo1<!>(<!NI;TYPE_MISMATCH!>Out<CharSequence>()<!>)
    a.foo1<<!UPPER_BOUND_VIOLATED!>Out<CharSequence><!>>(<!NI;TYPE_MISMATCH!>Out()<!>)

    a.foo1(Out())
    a.foo1(Out<Nothing>())

    a.<!OI;TYPE_INFERENCE_INCORPORATION_ERROR!>foo2<!>(<!NI;TYPE_MISMATCH!><!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Inv<!>()<!>)
    a.<!OI;TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>foo2<!>(<!NI;TYPE_MISMATCH!>Inv<CharSequence>()<!>)
    a.foo2<<!UPPER_BOUND_VIOLATED!>Inv<CharSequence><!>>(<!NI;TYPE_MISMATCH!>Inv()<!>)

    a.foo3(In())
    a.foo3(In<CharSequence>())
    a.foo3<In<CharSequence>>(In())

    b.foo1(Out())
    b.foo1(Out<CharSequence>())
    b.foo1<Out<CharSequence>>(Out())

    b.<!OI;TYPE_INFERENCE_INCORPORATION_ERROR!>foo2<!>(<!NI;TYPE_MISMATCH!><!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Inv<!>()<!>)
    b.<!OI;TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>foo2<!>(<!NI;TYPE_MISMATCH!>Inv<CharSequence>()<!>)
    b.foo2<<!UPPER_BOUND_VIOLATED!>Inv<CharSequence><!>>(<!NI;TYPE_MISMATCH!>Inv()<!>)


    b.<!OI;TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>foo3<!>(<!NI;TYPE_MISMATCH!>In<CharSequence>()<!>)
    b.foo3<<!UPPER_BOUND_VIOLATED!>In<CharSequence><!>>(<!NI;TYPE_MISMATCH!>In()<!>)

    b.foo3(In<Any?>())
    b.foo3(In())
}
