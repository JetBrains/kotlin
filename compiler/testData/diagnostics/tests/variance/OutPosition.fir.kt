// !LANGUAGE: -TrailingCommas

interface In<in T>
interface Out<out T>
interface Inv<T>
interface Pair<out X, out Y>

interface Test<in I, out O, P> {
    fun ok1(): O
    fun ok2(): In<I>
    fun ok3(): In<In<O>>
    fun ok4(): Inv<P>
    fun ok5(): P
    fun ok6(): Out<O>
    fun ok7(): Out<P>
    fun ok8(): Out<In<P>>
    fun ok9(): Pair<In<I>, O>
    
    fun ok10(): Inv<in I>
    fun ok11(): Inv<out O>
    fun ok12(): Inv<in P>
    fun ok13(): Inv<out P>

    fun neOk1(): I
    fun neOk2(): In<O>
    fun neOk3(): In<In<I>>
    fun neOk4(): Inv<I>
    fun neOk5(): Inv<O>
    fun neOk6(): Pair<In<O>, I>
    fun neOk7(): Inv<in O>
    fun neOk8(): <!CONFLICTING_PROJECTION!>Out<in I><!>
    
    fun neOk10(): Inv<in O>
    fun neOk11(): Inv<out I>

    fun neOk30(): Pair<I, >
    fun neOk31(): Pair<I, Inv>
    fun neOk32(): Inv
    fun neOk33(): Inv<<!SYNTAX!><!>>
    fun neOk34(): <!UNRESOLVED_REFERENCE!>Inv<C><!>
    fun neOk35(): <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inv<P, P><!>
}