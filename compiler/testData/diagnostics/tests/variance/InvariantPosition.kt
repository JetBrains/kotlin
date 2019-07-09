interface In<in T>
interface Out<out T>
interface Inv<T>
interface Pair<out X, out Y>

interface Test<in I, out O, P> {
    var ok1: Inv<P>
    var ok2: P
    var ok3: Out<In<P>>
    var ok4: Pair<In<P>, Out<P>>
    var ok5: Inv<out P>
    var ok6: Inv<in P>
    var ok7: Inv<out P>

    var neOk1: <!TYPE_VARIANCE_CONFLICT("O", "out", "invariant", "O")!>O<!>
    var neOk2: In<<!TYPE_VARIANCE_CONFLICT("I", "in", "invariant", "In<I>")!>I<!>>
    var neOk3: In<In<<!TYPE_VARIANCE_CONFLICT("O", "out", "invariant", "In<In<O>>")!>O<!>>>
    var neOk4: Inv<<!TYPE_VARIANCE_CONFLICT("I", "in", "invariant", "Inv<I>")!>I<!>>
    var neOk5: Inv<<!TYPE_VARIANCE_CONFLICT("O", "out", "invariant", "Inv<O>")!>O<!>>
    var neOk6: In<In<<!TYPE_VARIANCE_CONFLICT("O", "out", "invariant", "In<In<O>>")!>O<!>>>
    var neOk7: Pair<In<<!TYPE_VARIANCE_CONFLICT("I", "in", "invariant", "Pair<In<I>, O>")!>I<!>>, <!TYPE_VARIANCE_CONFLICT("O", "out", "invariant", "Pair<In<I>, O>")!>O<!>>
    var neOk8: Inv<in <!TYPE_VARIANCE_CONFLICT("O", "out", "invariant", "Inv<in O>")!>O<!>>
    var neOk9: Inv<in <!TYPE_VARIANCE_CONFLICT("I", "in", "invariant", "Inv<in I>")!>I<!>>
    var neOk10: In<<!CONFLICTING_PROJECTION!>out<!> I>

    var neOk11: <!TYPE_VARIANCE_CONFLICT("I", "in", "invariant", "I")!>I<!>
    var neOk12: In<<!TYPE_VARIANCE_CONFLICT("O", "out", "invariant", "In<O>")!>O<!>>
    var neOk13: In<In<<!TYPE_VARIANCE_CONFLICT("I", "in", "invariant", "In<In<I>>")!>I<!>>>
    var neOk14: Out<<!TYPE_VARIANCE_CONFLICT("I", "in", "invariant", "Out<I>")!>I<!>>
    var neOk15: Out<Out<<!TYPE_VARIANCE_CONFLICT("I", "in", "invariant", "Out<Out<I>>")!>I<!>>>
    var neOk16: Out<In<<!TYPE_VARIANCE_CONFLICT("O", "out", "invariant", "Out<In<O>>")!>O<!>>>
    var neOk17: Pair<In<<!TYPE_VARIANCE_CONFLICT("O", "out", "invariant", "Pair<In<O>, I>")!>O<!>>, <!TYPE_VARIANCE_CONFLICT("I", "in", "invariant", "Pair<In<O>, I>")!>I<!>>

    var neOk20: Inv<in <!TYPE_VARIANCE_CONFLICT("O", "out", "invariant", "Inv<in O>")!>O<!>>
    var neOk21: Inv<in <!TYPE_VARIANCE_CONFLICT("I", "in", "invariant", "Inv<in I>")!>I<!>>
    var neOk22: Inv<out <!TYPE_VARIANCE_CONFLICT("O", "out", "invariant", "Inv<out O>")!>O<!>>
    var neOk23: Inv<out <!TYPE_VARIANCE_CONFLICT("I", "in", "invariant", "Inv<out I>")!>I<!>>

    var neOk30: Pair<<!TYPE_VARIANCE_CONFLICT("I", "in", "invariant", "Pair<I, [ERROR : No type element]>")!>I<!>, <!SYNTAX!><!>>
    var neOk31: Pair<<!TYPE_VARIANCE_CONFLICT("I", "in", "invariant", "Pair<I, [ERROR : Inv]>")!>I<!>, <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inv<!>>
    var neOk32: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inv<!>
    var neOk33: Inv<<!SYNTAX!><!>>
    var neOk34: Inv<<!UNRESOLVED_REFERENCE!>C<!>>
    var neOk35: Inv<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><P, P><!>
}