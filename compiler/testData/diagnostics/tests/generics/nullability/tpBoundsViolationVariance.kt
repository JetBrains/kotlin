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
        fooInv2<<!UPPER_BOUND_VIOLATED!>Inv<F><!>>(Inv<F>())
        fooInv1(Inv<F>())
        <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED{OI}!>fooInv2<!>(<!TYPE_MISMATCH{NI}!>Inv<F>()<!>)

        fooIn1<In<F?>>(In<F?>())
        fooIn2<In<F?>>(In<F?>())
        fooIn1(In<F?>())
        fooIn2(In<F?>())

        fooOut1<Out<F>>(Out<F>())
        fooOut2<Out<F>>(Out<F>())
        fooOut1(Out<F>())
        fooOut2(Out<F>())

        // Z
        fooInv1<<!UPPER_BOUND_VIOLATED!>Inv<Z><!>>(Inv<Z>())
        fooInv2<<!UPPER_BOUND_VIOLATED!>Inv<Z><!>>(Inv<Z>())
        <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED{OI}!>fooInv1<!>(<!TYPE_MISMATCH{NI}!>Inv<Z>()<!>)
        <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED{OI}!>fooInv2<!>(<!TYPE_MISMATCH{NI}!>Inv<Z>()<!>)

        fooIn1<<!UPPER_BOUND_VIOLATED!>In<Z?><!>>(In<Z?>())
        fooIn2<<!UPPER_BOUND_VIOLATED!>In<Z?><!>>(In<Z?>())
        <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED{OI}!>fooIn1<!>(<!TYPE_MISMATCH{NI}!>In<Z?>()<!>)
        <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED{OI}!>fooIn2<!>(<!TYPE_MISMATCH{NI}!>In<Z?>()<!>)

        fooOut1<Out<Z>>(Out<Z>())
        fooOut2<Out<Z>>(Out<Z>())
        fooOut1(Out<Z>())
        fooOut2(Out<Z>())

        // W
        fooInv1<<!UPPER_BOUND_VIOLATED!>Inv<W><!>>(Inv<W>())
        fooInv2<<!UPPER_BOUND_VIOLATED!>Inv<W><!>>(Inv<W>())
        <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED{OI}!>fooInv1<!>(<!TYPE_MISMATCH{NI}!>Inv<W>()<!>)
        <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED{OI}!>fooInv2<!>(<!TYPE_MISMATCH{NI}!>Inv<W>()<!>)

        fooIn1<<!UPPER_BOUND_VIOLATED!>In<W?><!>>(In<W?>())
        fooIn2<<!UPPER_BOUND_VIOLATED!>In<W?><!>>(In<W?>())
        <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED{OI}!>fooIn1<!>(<!TYPE_MISMATCH{NI}!>In<W?>()<!>)
        <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED{OI}!>fooIn2<!>(<!TYPE_MISMATCH{NI}!>In<W?>()<!>)

        fooOut1<<!UPPER_BOUND_VIOLATED!>Out<W><!>>(Out<W>())
        fooOut2<Out<W>>(Out<W>())
        <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED{OI}!>fooOut1<!>(<!TYPE_MISMATCH{NI}!>Out<W>()<!>)
        fooOut2(Out<W>())
    }
}
