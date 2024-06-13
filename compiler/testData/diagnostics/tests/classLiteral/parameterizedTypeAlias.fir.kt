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
    Mixed::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Mixed<String>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Mixed<in String>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Mixed<out String>::class<!>

    Mixed2::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Mixed2<String>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Mixed2<in String>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Mixed2<out String>::class<!>

    UpperBound::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBound<Int, Long>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBound<in Int, out Long>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBound<out Int, out Long>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBound<in Int, in Long>::class<!>

    UpperBoundTypeAlias::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBoundTypeAlias<Int, Long>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBoundTypeAlias<in Int, out Long>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBoundTypeAlias<out Int, in Long>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBoundTypeAlias<out Int, out Long>::class<!>


    UpperBoundOutInTypealias::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBoundOutInTypealias<Int, Long>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBoundOutInTypealias<in Int, out Long>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBoundOutInTypealias<in Int, in Long>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBoundOutInTypealias<out Int, out Long>::class<!>

    UpperBoundTypeAliasUnused::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBoundTypeAliasUnused<String, String>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>UpperBoundTypeAliasUnused<in String, out String>::class<!>

    Inv::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Inv<String>::class<!>

    InvAlias::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>InvAlias<String>::class<!>

    InvUnused::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>InvUnused<String>::class<!>

    Some::class
    SomeAlias::class
    SomeAlias<String>::class

    MyPair::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>MyPair<Int, Int>::class<!>

    PairAliasSingle::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>PairAliasSingle<Int>::class<!>

    PairAliasReversed::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>PairAliasReversed<Int, Int>::class<!>

    PairAliasUsual::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>PairAliasUsual<Int, Int>::class<!>

    PairAliasSpecific::class

    PairAliasTwoWithUnused::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>PairAliasTwoWithUnused<Int, Int>::class<!>

    Array::class
    Array<Int>::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Array<*>::class<!>

    SimpleArrayAlias::class
    SimpleArrayAlias<Int>::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>SimpleArrayAlias<*>::class<!>

    SpecificArrayAlias::class

    UnusedArrayAlias::class
    UnusedArrayAlias<Int>::class
    UnusedArrayAlias<*>::class
}
