class Inv<T>
class Some
class MyPair<A, B>

typealias InvAlias<T> = Inv<T>
typealias InvUnused<T> = Inv<Int>
typealias SomeAlias<T> = Some
typealias PairAliasSingle<T> = MyPair<T, T>
typealias PairAliasUsual<A, B> = MyPair<A, B>
typealias PairAliasReversed<A, B> = MyPair<B, A>
typealias PairAliasTwoWithUnused<A, B> = MyPair<A, A>
typealias PairAliasSpecific = MyPair<Int, Int>
typealias SimpleArrayAlias<T> = Array<T>
typealias SpecificArrayAlias = Array<Int>
typealias UnusedArrayAlias<T> = Array<Int>

typealias Mixed<T> = Inv<MyPair<T, T>>
typealias Mixed2<T> = MyPair<Inv<T>, Inv<T>>

class UpperBound<A, T : A>
typealias UpperBoundTypeAlias<A, T> = UpperBound<A, T>
typealias UpperBoundTypeAliasUnused<A, T> = UpperBound<Int, <!UPPER_BOUND_VIOLATED!>Long<!>>

class UpperBoundOutIn<out A, in T : A>
typealias UpperBoundOutInTypealias<A, T> = UpperBoundOutIn<A, T>

fun test() {
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Mixed<!>::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Mixed<String>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Mixed<in String>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Mixed<out String>::class<!>

    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Mixed2<!>::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Mixed2<String>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Mixed2<in String>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Mixed2<out String>::class<!>

    UpperBound::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBound<Int, <!UPPER_BOUND_VIOLATED!>Long<!>>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBound<in Int, out <!UPPER_BOUND_VIOLATED!>Long<!>>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBound<out Int, out <!UPPER_BOUND_VIOLATED!>Long<!>>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBound<in Int, in <!UPPER_BOUND_VIOLATED!>Long<!>>::class<!>

    UpperBoundTypeAlias::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBoundTypeAlias<Int, <!UPPER_BOUND_VIOLATED!>Long<!>>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBoundTypeAlias<in Int, out <!UPPER_BOUND_VIOLATED!>Long<!>>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBoundTypeAlias<out Int, in <!UPPER_BOUND_VIOLATED!>Long<!>>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBoundTypeAlias<out Int, out <!UPPER_BOUND_VIOLATED!>Long<!>>::class<!>


    UpperBoundOutInTypealias::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBoundOutInTypealias<Int, <!UPPER_BOUND_VIOLATED!>Long<!>>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!><!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION, CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>UpperBoundOutInTypealias<in Int, out <!UPPER_BOUND_VIOLATED!>Long<!>><!>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!><!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>UpperBoundOutInTypealias<in Int, in <!UPPER_BOUND_VIOLATED!>Long<!>><!>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!><!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>UpperBoundOutInTypealias<out Int, out <!UPPER_BOUND_VIOLATED!>Long<!>><!>::class<!>

    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>UpperBoundTypeAliasUnused<!>::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBoundTypeAliasUnused<String, String>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBoundTypeAliasUnused<in String, out String>::class<!>

    Inv::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Inv<String>::class<!>

    InvAlias::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>InvAlias<String>::class<!>

    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>InvUnused<!>::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>InvUnused<String>::class<!>

    Some::class
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>SomeAlias<!>::class
    SomeAlias<String>::class

    MyPair::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>MyPair<Int, Int>::class<!>

    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>PairAliasSingle<!>::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>PairAliasSingle<Int>::class<!>

    PairAliasReversed::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>PairAliasReversed<Int, Int>::class<!>

    PairAliasUsual::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>PairAliasUsual<Int, Int>::class<!>

    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>PairAliasSpecific::class<!>

    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>PairAliasTwoWithUnused<!>::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>PairAliasTwoWithUnused<Int, Int>::class<!>

    Array::class
    Array<Int>::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Array<*>::class<!>

    SimpleArrayAlias::class
    SimpleArrayAlias<Int>::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>SimpleArrayAlias<*>::class<!>

    SpecificArrayAlias::class

    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>UnusedArrayAlias<!>::class
    UnusedArrayAlias<Int>::class
    UnusedArrayAlias<*>::class
}
