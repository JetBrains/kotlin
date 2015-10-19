// See KT-9438: Enforce the Single Instantiation Inheritance Rule for type parameters

interface A

interface B

interface D<T>

interface IncorrectF<<!MISPLACED_TYPE_PARAMETER_CONSTRAINTS, INCONSISTENT_TYPE_PARAMETER_BOUNDS!>T : D<A><!>> where T : D<B>

interface CorrectF<<!INCONSISTENT_TYPE_PARAMETER_BOUNDS!>T<!>> where T : D<A>, T : D<B>

interface G<T>

interface IncorrectH<<!MISPLACED_TYPE_PARAMETER_CONSTRAINTS, INCONSISTENT_TYPE_PARAMETER_BOUNDS!>T : G<D<A>><!>> where T : G<D<T>>

interface CorrectH<<!INCONSISTENT_TYPE_PARAMETER_BOUNDS!>T<!>> where T : G<D<A>>, T : G<D<B>>

interface I<T : G<D<T>>> {
    fun <<!MISPLACED_TYPE_PARAMETER_CONSTRAINTS, INCONSISTENT_TYPE_PARAMETER_BOUNDS!>S : T?<!>> incorrectFoo() where S : G<D<S>>

    fun <<!INCONSISTENT_TYPE_PARAMETER_BOUNDS!>S<!>> correctFoo() where S : T?, S : G<D<S>>
}

interface incorrectJ<<!MISPLACED_TYPE_PARAMETER_CONSTRAINTS, INCONSISTENT_TYPE_PARAMETER_BOUNDS!>T: G<D<T>><!>> where T : G<D<T?>>

interface correctJ<<!INCONSISTENT_TYPE_PARAMETER_BOUNDS!>T<!>> where T : G<D<T>>, T : G<D<T?>>

fun <<!INCONSISTENT_TYPE_PARAMETER_BOUNDS!>T<!>> bar() where T : D<A>, T : D<B> {}