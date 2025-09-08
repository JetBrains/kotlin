// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE
// DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE

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
        <!INAPPLICABLE_CANDIDATE!>fooInv2<!><<!UPPER_BOUND_VIOLATED!>Inv<F><!>>(Inv<F>())
        fooInv1(Inv<F>())
        <!CANNOT_INFER_PARAMETER_TYPE!>fooInv2<!>(<!ARGUMENT_TYPE_MISMATCH!>Inv<F>()<!>)

        fooIn1<In<F?>>(In<F?>())
        fooIn2<In<F?>>(In<F?>())
        fooIn1(In<F?>())
        fooIn2(In<F?>())

        fooOut1<Out<F>>(Out<F>())
        fooOut2<Out<F>>(Out<F>())
        fooOut1(Out<F>())
        fooOut2(Out<F>())

        // Z
        <!INAPPLICABLE_CANDIDATE!>fooInv1<!><<!UPPER_BOUND_VIOLATED!>Inv<Z><!>>(Inv<Z>())
        <!INAPPLICABLE_CANDIDATE!>fooInv2<!><<!UPPER_BOUND_VIOLATED!>Inv<Z><!>>(Inv<Z>())
        <!CANNOT_INFER_PARAMETER_TYPE!>fooInv1<!>(<!ARGUMENT_TYPE_MISMATCH!>Inv<Z>()<!>)
        <!CANNOT_INFER_PARAMETER_TYPE!>fooInv2<!>(<!ARGUMENT_TYPE_MISMATCH!>Inv<Z>()<!>)

        <!INAPPLICABLE_CANDIDATE!>fooIn1<!><<!UPPER_BOUND_VIOLATED!>In<Z?><!>>(In<Z?>())
        <!INAPPLICABLE_CANDIDATE!>fooIn2<!><<!UPPER_BOUND_VIOLATED!>In<Z?><!>>(In<Z?>())
        <!CANNOT_INFER_PARAMETER_TYPE!>fooIn1<!>(<!ARGUMENT_TYPE_MISMATCH!>In<Z?>()<!>)
        <!CANNOT_INFER_PARAMETER_TYPE!>fooIn2<!>(<!ARGUMENT_TYPE_MISMATCH!>In<Z?>()<!>)

        fooOut1<Out<Z>>(Out<Z>())
        fooOut2<Out<Z>>(Out<Z>())
        fooOut1(Out<Z>())
        fooOut2(Out<Z>())

        // W
        <!INAPPLICABLE_CANDIDATE!>fooInv1<!><<!UPPER_BOUND_VIOLATED!>Inv<W><!>>(Inv<W>())
        <!INAPPLICABLE_CANDIDATE!>fooInv2<!><<!UPPER_BOUND_VIOLATED!>Inv<W><!>>(Inv<W>())
        <!CANNOT_INFER_PARAMETER_TYPE!>fooInv1<!>(<!ARGUMENT_TYPE_MISMATCH!>Inv<W>()<!>)
        <!CANNOT_INFER_PARAMETER_TYPE!>fooInv2<!>(<!ARGUMENT_TYPE_MISMATCH!>Inv<W>()<!>)

        <!INAPPLICABLE_CANDIDATE!>fooIn1<!><<!UPPER_BOUND_VIOLATED!>In<W?><!>>(In<W?>())
        <!INAPPLICABLE_CANDIDATE!>fooIn2<!><<!UPPER_BOUND_VIOLATED!>In<W?><!>>(In<W?>())
        <!CANNOT_INFER_PARAMETER_TYPE!>fooIn1<!>(<!ARGUMENT_TYPE_MISMATCH!>In<W?>()<!>)
        <!CANNOT_INFER_PARAMETER_TYPE!>fooIn2<!>(<!ARGUMENT_TYPE_MISMATCH!>In<W?>()<!>)

        <!INAPPLICABLE_CANDIDATE!>fooOut1<!><<!UPPER_BOUND_VIOLATED!>Out<W><!>>(Out<W>())
        fooOut2<Out<W>>(Out<W>())
        <!CANNOT_INFER_PARAMETER_TYPE!>fooOut1<!>(<!ARGUMENT_TYPE_MISMATCH!>Out<W>()<!>)
        fooOut2(Out<W>())
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, in, infix,
nestedClass, nullableType, out, typeConstraint, typeParameter, typeWithExtension */
