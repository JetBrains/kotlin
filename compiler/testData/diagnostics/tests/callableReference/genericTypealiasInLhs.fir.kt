class Some { fun foo() {} }

typealias SomeAlias = Some
typealias SomeUnusedAlias<T> = Some

fun test_1() {
    Some::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Some<Int><!>::foo

    SomeAlias::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>SomeAlias<Int><!>::foo

    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>SomeUnusedAlias<!>::foo
    SomeUnusedAlias<Int>::foo
    SomeUnusedAlias<out Int>::foo
    SomeUnusedAlias<in Int>::foo
    SomeUnusedAlias<*>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>SomeUnusedAlias<Int, Int><!>::foo
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
    Inv<<!UPPER_BOUND_VIOLATED!>out Int<!>>::foo
    Inv<out String>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inv<!>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inv<String, String><!>::foo

    InvAlias<String>::foo
    InvAlias<<!UPPER_BOUND_VIOLATED!>Int<!>>::foo
    InvAlias<*>::foo
    InvAlias<<!UPPER_BOUND_VIOLATED!>out Int<!>>::foo
    InvAlias<out String>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>InvAlias<!>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>InvAlias<String, String><!>::foo

    InvUnusedCorrectAlias<String>::foo
    InvUnusedCorrectAlias<Int>::foo
    InvUnusedCorrectAlias<*>::foo
    InvUnusedCorrectAlias<out Int>::foo
    InvUnusedCorrectAlias<out String>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>InvUnusedCorrectAlias<!>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>InvUnusedCorrectAlias<String, String><!>::foo

    InvUnusedIncorrectAlias<String>::foo
    InvUnusedIncorrectAlias<Int>::foo
    InvUnusedIncorrectAlias<*>::foo
    InvUnusedIncorrectAlias<out Int>::foo
    InvUnusedIncorrectAlias<out String>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>InvUnusedIncorrectAlias<!>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>InvUnusedIncorrectAlias<String, String><!>::foo

    InvSpecificAlias::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>InvSpecificAlias<String, String><!>::foo
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
    BoundedPair<<!UPPER_BOUND_VIOLATED!>out Int<!>, out Int>::foo
    BoundedPair<in String, in Int>::foo
    BoundedPair<<!UPPER_BOUND_VIOLATED!>in Int<!>, in Int>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPair<!>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPair<Int, Int, Int><!>::foo

    BoundedPairAlias<String, Int>::foo
    BoundedPairAlias<<!UPPER_BOUND_VIOLATED!>Int<!>, Int>::foo
    BoundedPairAlias<*, *>::foo
    BoundedPairAlias<out String, out Int>::foo
    BoundedPairAlias<<!UPPER_BOUND_VIOLATED!>out Int<!>, out Int>::foo
    BoundedPairAlias<in String, in Int>::foo
    BoundedPairAlias<<!UPPER_BOUND_VIOLATED!>in Int<!>, in Int>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairAlias<!>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairAlias<Int, Int, Int><!>::foo

    BoundedPairInverted<String, <!UPPER_BOUND_VIOLATED!>Int<!>>::foo
    BoundedPairInverted<Int, <!UPPER_BOUND_VIOLATED!>Int<!>>::foo
    BoundedPairInverted<*, *>::foo
    BoundedPairInverted<out String, <!UPPER_BOUND_VIOLATED!>out Int<!>>::foo
    BoundedPairInverted<out Int, <!UPPER_BOUND_VIOLATED!>out Int<!>>::foo
    BoundedPairInverted<in String, <!UPPER_BOUND_VIOLATED!>in Int<!>>::foo
    BoundedPairInverted<in Int, <!UPPER_BOUND_VIOLATED!>in Int<!>>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairInverted<!>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairInverted<Int, Int, Int><!>::foo

    BoundedPairFirstFixedAlias<Int>::foo
    BoundedPairFirstFixedAlias<Int>::foo
    BoundedPairFirstFixedAlias<*>::foo
    BoundedPairFirstFixedAlias<out String>::foo
    BoundedPairFirstFixedAlias<out Int>::foo
    BoundedPairFirstFixedAlias<in String>::foo
    BoundedPairFirstFixedAlias<in Int>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairFirstFixedAlias<!>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairFirstFixedAlias<Int, Int><!>::foo

    BoundedPairSecondFixedAlias<<!UPPER_BOUND_VIOLATED!>Int<!>>::foo
    BoundedPairSecondFixedAlias<<!UPPER_BOUND_VIOLATED!>Int<!>>::foo
    BoundedPairSecondFixedAlias<*>::foo
    BoundedPairSecondFixedAlias<out String>::foo
    BoundedPairSecondFixedAlias<<!UPPER_BOUND_VIOLATED!>out Int<!>>::foo
    BoundedPairSecondFixedAlias<in String>::foo
    BoundedPairSecondFixedAlias<<!UPPER_BOUND_VIOLATED!>in Int<!>>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairSecondFixedAlias<!>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairSecondFixedAlias<Int, Int><!>::foo

    BoundedPairFirstUnusedAlias<String, Int>::foo
    BoundedPairFirstUnusedAlias<Int, Int>::foo
    BoundedPairFirstUnusedAlias<*, *>::foo
    BoundedPairFirstUnusedAlias<out String, out Int>::foo
    BoundedPairFirstUnusedAlias<out Int, out Int>::foo
    BoundedPairFirstUnusedAlias<in String, in Int>::foo
    BoundedPairFirstUnusedAlias<in Int, in Int>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairFirstUnusedAlias<!>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairFirstUnusedAlias<Int, Int, Int><!>::foo

    BoundedPairSecondUnusedAlias<String, Int>::foo
    BoundedPairSecondUnusedAlias<<!UPPER_BOUND_VIOLATED!>Int<!>, Int>::foo
    BoundedPairSecondUnusedAlias<*, *>::foo
    BoundedPairSecondUnusedAlias<out String, out Int>::foo
    BoundedPairSecondUnusedAlias<<!UPPER_BOUND_VIOLATED!>out Int<!>, out Int>::foo
    BoundedPairSecondUnusedAlias<in String, in Int>::foo
    BoundedPairSecondUnusedAlias<<!UPPER_BOUND_VIOLATED!>in Int<!>, in Int>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairSecondUnusedAlias<!>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairSecondUnusedAlias<Int, Int, Int><!>::foo

    BoundedPairBothAlias<String, Int>::foo
    BoundedPairBothAlias<Int, Int>::foo
    BoundedPairBothAlias<*, *>::foo
    BoundedPairBothAlias<out String, out Int>::foo
    BoundedPairBothAlias<out Int, out Int>::foo
    BoundedPairBothAlias<in String, in Int>::foo
    BoundedPairBothAlias<in Int, in Int>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairBothAlias<!>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairBothAlias<Int, Int, Int><!>::foo

    BoundedPairSpecificAlias<String, Int>::foo
    BoundedPairSpecificAlias<Int, Int>::foo
    BoundedPairSpecificAlias<*, *>::foo
    BoundedPairSpecificAlias<out String, out Int>::foo
    BoundedPairSpecificAlias<out Int, out Int>::foo
    BoundedPairSpecificAlias<in String, in Int>::foo
    BoundedPairSpecificAlias<in Int, in Int>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairSpecificAlias<!>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>BoundedPairSpecificAlias<Int, Int, Int><!>::foo
}
