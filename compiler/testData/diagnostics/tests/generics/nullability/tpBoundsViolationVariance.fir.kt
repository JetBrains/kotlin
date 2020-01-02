// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE

class A<F> {
    class Inv<Q>
    fun <E : Inv<F>> fooInv1(x: E) = x
    fun <E : Inv<F?>> fooInv2(x: E) = x

    class In<in Q>
    fun <E : In<F>> fooIn1(x: E) = x
    fun <E : In<F?>> fooIn2(x: E) = x

    class Out<out Q>
    fun <E : Out<F>> fooOut1(x: E) = x
    fun <E : Out<F?>> fooOut2(x: E) = x

    fun <Z : F, W : Z?> bar() {
        // F
        fooInv1<Inv<F>>(Inv<F>())
        <!INAPPLICABLE_CANDIDATE!>fooInv2<!><Inv<F>>(Inv<F>())
        fooInv1(Inv<F>())
        <!INAPPLICABLE_CANDIDATE!>fooInv2<!>(Inv<F>())

        fooIn1<In<F?>>(In<F?>())
        fooIn2<In<F?>>(In<F?>())
        fooIn1(In<F?>())
        fooIn2(In<F?>())

        fooOut1<Out<F>>(Out<F>())
        fooOut2<Out<F>>(Out<F>())
        fooOut1(Out<F>())
        fooOut2(Out<F>())

        // Z
        <!INAPPLICABLE_CANDIDATE!>fooInv1<!><Inv<Z>>(Inv<Z>())
        <!INAPPLICABLE_CANDIDATE!>fooInv2<!><Inv<Z>>(Inv<Z>())
        <!INAPPLICABLE_CANDIDATE!>fooInv1<!>(Inv<Z>())
        <!INAPPLICABLE_CANDIDATE!>fooInv2<!>(Inv<Z>())

        <!INAPPLICABLE_CANDIDATE!>fooIn1<!><In<Z?>>(In<Z?>())
        <!INAPPLICABLE_CANDIDATE!>fooIn2<!><In<Z?>>(In<Z?>())
        <!INAPPLICABLE_CANDIDATE!>fooIn1<!>(In<Z?>())
        <!INAPPLICABLE_CANDIDATE!>fooIn2<!>(In<Z?>())

        fooOut1<Out<Z>>(Out<Z>())
        fooOut2<Out<Z>>(Out<Z>())
        fooOut1(Out<Z>())
        fooOut2(Out<Z>())

        // W
        <!INAPPLICABLE_CANDIDATE!>fooInv1<!><Inv<W>>(Inv<W>())
        <!INAPPLICABLE_CANDIDATE!>fooInv2<!><Inv<W>>(Inv<W>())
        <!INAPPLICABLE_CANDIDATE!>fooInv1<!>(Inv<W>())
        <!INAPPLICABLE_CANDIDATE!>fooInv2<!>(Inv<W>())

        <!INAPPLICABLE_CANDIDATE!>fooIn1<!><In<W?>>(In<W?>())
        <!INAPPLICABLE_CANDIDATE!>fooIn2<!><In<W?>>(In<W?>())
        <!INAPPLICABLE_CANDIDATE!>fooIn1<!>(In<W?>())
        <!INAPPLICABLE_CANDIDATE!>fooIn2<!>(In<W?>())

        <!INAPPLICABLE_CANDIDATE!>fooOut1<!><Out<W>>(Out<W>())
        fooOut2<Out<W>>(Out<W>())
        <!INAPPLICABLE_CANDIDATE!>fooOut1<!>(Out<W>())
        fooOut2(Out<W>())
    }
}
