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
    a.<!TYPE_INFERENCE_UPPER_BOUND_VIOLATED{OI}!>foo1<!>(<!TYPE_MISMATCH{NI}!>Out<CharSequence>()<!>)
    a.foo1<<!UPPER_BOUND_VIOLATED!>Out<CharSequence><!>>(<!TYPE_MISMATCH{NI}!>Out()<!>)

    a.foo1(Out())
    a.foo1(Out<Nothing>())

    a.<!TYPE_INFERENCE_INCORPORATION_ERROR{OI}!>foo2<!>(<!TYPE_MISMATCH{NI}!><!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>Inv<!>()<!>)
    a.<!TYPE_INFERENCE_UPPER_BOUND_VIOLATED{OI}!>foo2<!>(<!TYPE_MISMATCH{NI}!>Inv<CharSequence>()<!>)
    a.foo2<<!UPPER_BOUND_VIOLATED!>Inv<CharSequence><!>>(<!TYPE_MISMATCH{NI}!>Inv()<!>)

    a.foo3(In())
    a.foo3(In<CharSequence>())
    a.foo3<In<CharSequence>>(In())

    b.foo1(Out())
    b.foo1(Out<CharSequence>())
    b.foo1<Out<CharSequence>>(Out())

    b.<!TYPE_INFERENCE_INCORPORATION_ERROR{OI}!>foo2<!>(<!TYPE_MISMATCH{NI}!><!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>Inv<!>()<!>)
    b.<!TYPE_INFERENCE_UPPER_BOUND_VIOLATED{OI}!>foo2<!>(<!TYPE_MISMATCH{NI}!>Inv<CharSequence>()<!>)
    b.foo2<<!UPPER_BOUND_VIOLATED!>Inv<CharSequence><!>>(<!TYPE_MISMATCH{NI}!>Inv()<!>)


    b.<!TYPE_INFERENCE_UPPER_BOUND_VIOLATED{OI}!>foo3<!>(<!TYPE_MISMATCH{NI}!>In<CharSequence>()<!>)
    b.foo3<<!UPPER_BOUND_VIOLATED!>In<CharSequence><!>>(<!TYPE_MISMATCH{NI}!>In()<!>)

    b.foo3(In<Any?>())
    b.foo3(In())
}
