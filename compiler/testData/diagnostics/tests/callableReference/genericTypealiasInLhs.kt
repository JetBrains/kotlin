class Some { fun foo() {} }

typealias SomeAlias = Some
typealias SomeUnusedAlias<T> = Some

fun test_1() {
    Some::foo
    Some<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>

    SomeAlias::foo
    SomeAlias<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>

    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>SomeUnusedAlias<!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>
    SomeUnusedAlias<Int>::foo
    SomeUnusedAlias<out Int>::foo
    SomeUnusedAlias<in Int>::foo
    SomeUnusedAlias<*>::foo
    SomeUnusedAlias<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>
}

// ----------------------------------------------------------------

class Inv<T : CharSequence> { fun foo() {} }

typealias InvAlias<T> = Inv<T>
typealias InvUnusedCorrectAlias<T> = Inv<String>
typealias InvUnusedIncorrectAlias<T> = Inv<<!UPPER_BOUND_VIOLATED!>Int<!>>
typealias InvSpecificAlias = Inv<String>

fun test_2() {
    Inv<String>::foo
    Inv<<!UPPER_BOUND_VIOLATED!>Int<!>>::foo
    Inv<*>::foo
    Inv<out <!UPPER_BOUND_VIOLATED!>Int<!>>::foo
    Inv<out String>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inv<!>::foo
    Inv<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, String><!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>

    InvAlias<String>::foo
    InvAlias<<!UPPER_BOUND_VIOLATED!>Int<!>>::foo
    InvAlias<*>::foo
    InvAlias<out <!UPPER_BOUND_VIOLATED!>Int<!>>::foo
    InvAlias<out String>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>InvAlias<!>::foo
    InvAlias<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, String><!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>

    InvUnusedCorrectAlias<String>::foo
    InvUnusedCorrectAlias<Int>::foo
    InvUnusedCorrectAlias<*>::foo
    InvUnusedCorrectAlias<out Int>::foo
    InvUnusedCorrectAlias<out String>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>InvUnusedCorrectAlias<!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>
    InvUnusedCorrectAlias<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, String><!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>

    InvUnusedIncorrectAlias<String>::foo
    InvUnusedIncorrectAlias<Int>::foo
    InvUnusedIncorrectAlias<*>::foo
    InvUnusedIncorrectAlias<out Int>::foo
    InvUnusedIncorrectAlias<out String>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>InvUnusedIncorrectAlias<!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>
    InvUnusedIncorrectAlias<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, String><!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>

    InvSpecificAlias::foo
    InvSpecificAlias<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, String><!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>
}

// ----------------------------------------------------------------

class BoundedPair<T : CharSequence, Q> { fun foo() {} }

typealias BoundedPairAlias<T, Q> = BoundedPair<T, Q>
typealias BoundedPairInverted<Q, T> = BoundedPair<T, Q>
typealias BoundedPairFirstFixedAlias<Q> = BoundedPair<String, Q>
typealias BoundedPairSecondFixedAlias<T> = BoundedPair<T, Int>
typealias BoundedPairFirstUnusedAlias<T, Q> = BoundedPair<String, Q>
typealias BoundedPairSecondUnusedAlias<T, Q> = BoundedPair<T, Int>
typealias BoundedPairBothAlias<T, Q> = BoundedPair<String, Int>
typealias BoundedPairSpecificAlias<T, Q> = BoundedPair<String, Int>

fun test_3() {
    BoundedPair<String, Int>::foo
    BoundedPair<<!UPPER_BOUND_VIOLATED!>Int<!>, Int>::foo
    BoundedPair<*, *>::foo
    BoundedPair<out String, out Int>::foo
    BoundedPair<out <!UPPER_BOUND_VIOLATED!>Int<!>, out Int>::foo
    BoundedPair<in String, in Int>::foo
    BoundedPair<in <!UPPER_BOUND_VIOLATED!>Int<!>, in Int>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPair<!>::foo
    BoundedPair<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int, Int><!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>

    BoundedPairAlias<String, Int>::foo
    BoundedPairAlias<<!UPPER_BOUND_VIOLATED!>Int<!>, Int>::foo
    BoundedPairAlias<*, *>::foo
    BoundedPairAlias<out String, out Int>::foo
    BoundedPairAlias<out <!UPPER_BOUND_VIOLATED!>Int<!>, out Int>::foo
    BoundedPairAlias<in String, in Int>::foo
    BoundedPairAlias<in <!UPPER_BOUND_VIOLATED!>Int<!>, in Int>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairAlias<!>::foo
    BoundedPairAlias<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int, Int><!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>

    BoundedPairInverted<String, <!UPPER_BOUND_VIOLATED!>Int<!>>::foo
    BoundedPairInverted<Int, <!UPPER_BOUND_VIOLATED!>Int<!>>::foo
    BoundedPairInverted<*, *>::foo
    BoundedPairInverted<out String, out <!UPPER_BOUND_VIOLATED!>Int<!>>::foo
    BoundedPairInverted<out Int, out <!UPPER_BOUND_VIOLATED!>Int<!>>::foo
    BoundedPairInverted<in String, in <!UPPER_BOUND_VIOLATED!>Int<!>>::foo
    BoundedPairInverted<in Int, in <!UPPER_BOUND_VIOLATED!>Int<!>>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairInverted<!>::foo
    BoundedPairInverted<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int, Int><!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>

    BoundedPairFirstFixedAlias<Int>::foo
    BoundedPairFirstFixedAlias<Int>::foo
    BoundedPairFirstFixedAlias<*>::foo
    BoundedPairFirstFixedAlias<out String>::foo
    BoundedPairFirstFixedAlias<out Int>::foo
    BoundedPairFirstFixedAlias<in String>::foo
    BoundedPairFirstFixedAlias<in Int>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairFirstFixedAlias<!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>
    BoundedPairFirstFixedAlias<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>

    BoundedPairSecondFixedAlias<<!UPPER_BOUND_VIOLATED!>Int<!>>::foo
    BoundedPairSecondFixedAlias<<!UPPER_BOUND_VIOLATED!>Int<!>>::foo
    BoundedPairSecondFixedAlias<*>::foo
    BoundedPairSecondFixedAlias<out String>::foo
    BoundedPairSecondFixedAlias<out <!UPPER_BOUND_VIOLATED!>Int<!>>::foo
    BoundedPairSecondFixedAlias<in String>::foo
    BoundedPairSecondFixedAlias<in <!UPPER_BOUND_VIOLATED!>Int<!>>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairSecondFixedAlias<!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>
    BoundedPairSecondFixedAlias<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>

    BoundedPairFirstUnusedAlias<String, Int>::foo
    BoundedPairFirstUnusedAlias<Int, Int>::foo
    BoundedPairFirstUnusedAlias<*, *>::foo
    BoundedPairFirstUnusedAlias<out String, out Int>::foo
    BoundedPairFirstUnusedAlias<out Int, out Int>::foo
    BoundedPairFirstUnusedAlias<in String, in Int>::foo
    BoundedPairFirstUnusedAlias<in Int, in Int>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairFirstUnusedAlias<!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>
    BoundedPairFirstUnusedAlias<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int, Int><!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>

    BoundedPairSecondUnusedAlias<String, Int>::foo
    BoundedPairSecondUnusedAlias<<!UPPER_BOUND_VIOLATED!>Int<!>, Int>::foo
    BoundedPairSecondUnusedAlias<*, *>::foo
    BoundedPairSecondUnusedAlias<out String, out Int>::foo
    BoundedPairSecondUnusedAlias<out <!UPPER_BOUND_VIOLATED!>Int<!>, out Int>::foo
    BoundedPairSecondUnusedAlias<in String, in Int>::foo
    BoundedPairSecondUnusedAlias<in <!UPPER_BOUND_VIOLATED!>Int<!>, in Int>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairSecondUnusedAlias<!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>
    BoundedPairSecondUnusedAlias<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int, Int><!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>

    BoundedPairBothAlias<String, Int>::foo
    BoundedPairBothAlias<Int, Int>::foo
    BoundedPairBothAlias<*, *>::foo
    BoundedPairBothAlias<out String, out Int>::foo
    BoundedPairBothAlias<out Int, out Int>::foo
    BoundedPairBothAlias<in String, in Int>::foo
    BoundedPairBothAlias<in Int, in Int>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairBothAlias<!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>
    BoundedPairBothAlias<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int, Int><!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>

    BoundedPairSpecificAlias<String, Int>::foo
    BoundedPairSpecificAlias<Int, Int>::foo
    BoundedPairSpecificAlias<*, *>::foo
    BoundedPairSpecificAlias<out String, out Int>::foo
    BoundedPairSpecificAlias<out Int, out Int>::foo
    BoundedPairSpecificAlias<in String, in Int>::foo
    BoundedPairSpecificAlias<in Int, in Int>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairSpecificAlias<!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>
    BoundedPairSpecificAlias<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int, Int><!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>
}
