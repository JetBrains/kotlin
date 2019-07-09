interface In<in T>
interface Out<out T>
interface Inv<T>
interface Pair<out X, out Y>

interface Test<in I, out O, P> {
    fun ok1(i: I)
    fun ok2(i: In<O>)
    fun ok3(i: In<In<I>>)
    fun ok4(i: Inv<P>)
    fun ok5(i: P)
    fun ok6(i: Out<I>)
    fun ok7(i: Out<Out<I>>)
    fun ok8(i: Out<In<O>>)
    fun ok9(i: Out<In<P>>)
    fun Ok10(i: I)
    fun Ok11(i: In<O>)
    fun Ok12(i: In<In<I>>)
    fun Ok13(i: Out<I>)
    fun Ok14(i: Pair<In<O>, I>)
    fun Ok15(i: Inv<out I>)

    fun Ok20(i: Inv<in O>)
    fun Ok21(i: Inv<in P>)
    fun Ok22(i: Inv<out I>)
    fun Ok23(i: Inv<out P>)

    fun neOk1(i: <!TYPE_VARIANCE_CONFLICT("O", "out", "in", "O")!>O<!>)
    fun neOk2(i: In<<!TYPE_VARIANCE_CONFLICT("I", "in", "out", "In<I>")!>I<!>>)
    fun neOk3(i: In<In<<!TYPE_VARIANCE_CONFLICT("O", "out", "in", "In<In<O>>")!>O<!>>>)
    fun neOk4(i: Inv<<!TYPE_VARIANCE_CONFLICT("I", "in", "invariant", "Inv<I>")!>I<!>>)
    fun neOk5(i: Inv<<!TYPE_VARIANCE_CONFLICT("O", "out", "invariant", "Inv<O>")!>O<!>>)
    fun neOk6(i: In<In<<!TYPE_VARIANCE_CONFLICT("O", "out", "in", "In<In<O>>")!>O<!>>>)
    fun neOk7(i: Pair<In<<!TYPE_VARIANCE_CONFLICT("I", "in", "out", "Pair<In<I>, O>")!>I<!>>, <!TYPE_VARIANCE_CONFLICT("O", "out", "in", "Pair<In<I>, O>")!>O<!>>)
    fun neOk8(i: Inv<out <!TYPE_VARIANCE_CONFLICT("O", "out", "in", "Inv<out O>")!>O<!>>)
    fun neOk9(i: In<<!CONFLICTING_PROJECTION!>out<!> P>)
    fun neOk10(i: Out<<!TYPE_VARIANCE_CONFLICT("O", "out", "in", "Out<O>")!>O<!>>)

    fun neOk11(i: Inv<in <!TYPE_VARIANCE_CONFLICT("I", "in", "out", "Inv<in I>")!>I<!>>)
    fun neOk12(i: Inv<out <!TYPE_VARIANCE_CONFLICT("O", "out", "in", "Inv<out O>")!>O<!>>)

    fun neOk30(i: Pair<<!TYPE_VARIANCE_CONFLICT("O", "out", "in", "Pair<O, [ERROR : No type element]>")!>O<!>, <!SYNTAX!><!>>)
    fun neOk31(i: Pair<<!TYPE_VARIANCE_CONFLICT("O", "out", "in", "Pair<O, [ERROR : Inv]>")!>O<!>, <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inv<!>>)
    fun neOk32(i: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inv<!>)
    fun neOk33(i: Inv<<!SYNTAX!><!>>)
    fun neOk34(i: Inv<<!UNRESOLVED_REFERENCE!>C<!>>)
    fun neOk35(i: Inv<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><P, P><!>)
}