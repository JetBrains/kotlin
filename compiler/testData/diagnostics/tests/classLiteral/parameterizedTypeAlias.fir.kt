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

fun test() {
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
