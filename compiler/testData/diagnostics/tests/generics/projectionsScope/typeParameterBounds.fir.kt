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
    a.<!INAPPLICABLE_CANDIDATE!>foo1<!>(Out<CharSequence>())
    a.<!INAPPLICABLE_CANDIDATE!>foo1<!><Out<CharSequence>>(Out())

    a.foo1(Out())
    a.foo1(Out<Nothing>())

    a.foo2(Inv())
    a.<!INAPPLICABLE_CANDIDATE!>foo2<!>(Inv<CharSequence>())
    a.<!INAPPLICABLE_CANDIDATE!>foo2<!><Inv<CharSequence>>(Inv())

    a.foo3(In())
    a.foo3(In<CharSequence>())
    a.foo3<In<CharSequence>>(In())

    b.foo1(Out())
    b.foo1(Out<CharSequence>())
    b.foo1<Out<CharSequence>>(Out())

    b.foo2(Inv())
    b.<!INAPPLICABLE_CANDIDATE!>foo2<!>(Inv<CharSequence>())
    b.<!INAPPLICABLE_CANDIDATE!>foo2<!><Inv<CharSequence>>(Inv())


    b.<!INAPPLICABLE_CANDIDATE!>foo3<!>(In<CharSequence>())
    b.<!INAPPLICABLE_CANDIDATE!>foo3<!><In<CharSequence>>(In())

    b.foo3(In<Any?>())
    b.foo3(In())
}
