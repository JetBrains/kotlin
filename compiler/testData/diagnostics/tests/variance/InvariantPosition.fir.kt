// !LANGUAGE: -TrailingCommas

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

    var neOk1: O
    var neOk2: In<I>
    var neOk3: In<In<O>>
    var neOk4: Inv<I>
    var neOk5: Inv<O>
    var neOk6: In<In<O>>
    var neOk7: Pair<In<I>, O>
    var neOk8: Inv<in O>
    var neOk9: Inv<in I>
    var neOk10: <!CONFLICTING_PROJECTION!>In<out I><!>

    var neOk11: I
    var neOk12: In<O>
    var neOk13: In<In<I>>
    var neOk14: Out<I>
    var neOk15: Out<Out<I>>
    var neOk16: Out<In<O>>
    var neOk17: Pair<In<O>, I>

    var neOk20: Inv<in O>
    var neOk21: Inv<in I>
    var neOk22: Inv<out O>
    var neOk23: Inv<out I>

    var neOk30: Pair<I, >
    var neOk31: Pair<I, Inv>
    var neOk32: Inv
    var neOk33: Inv<<!SYNTAX!><!>>
    var neOk34: <!UNRESOLVED_REFERENCE!>Inv<C><!>
    var neOk35: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inv<P, P><!>
}