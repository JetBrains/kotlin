// See KT-9438: Enforce the Single Instantiation Inheritance Rule for type parameters

interface A

interface B

interface D<T>

interface IncorrectF<<!MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>T : D<A><!>> where T : <!REPEATED_BOUND!>D<B><!>

interface CorrectF<T> where T : D<A>, T : <!REPEATED_BOUND!>D<B><!>

interface G<T>

interface IncorrectH<<!MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>T : G<D<A>><!>> where T : <!REPEATED_BOUND!>G<D<T>><!>

interface CorrectH<T> where T : G<D<A>>, T : <!REPEATED_BOUND!>G<D<B>><!>

interface I<T : G<D<T>>> {
    fun <<!MISPLACED_TYPE_PARAMETER_CONSTRAINTS, INCONSISTENT_TYPE_PARAMETER_BOUNDS!>S : T?<!>> incorrectFoo() where S : G<D<S>>

    fun <<!INCONSISTENT_TYPE_PARAMETER_BOUNDS!>S<!>> correctFoo() where S : T?, S : G<D<S>>
}

interface incorrectJ<<!MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>T: G<D<T>><!>> where T : <!REPEATED_BOUND!>G<D<T?>><!>

interface correctJ<T> where T : G<D<T>>, T : <!REPEATED_BOUND!>G<D<T?>><!>

fun <T> bar() where T : D<A>, T : <!REPEATED_BOUND!>D<B><!> {}
