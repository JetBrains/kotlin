// LANGUAGE: -ProhibitSimplificationOfNonTrivialConstBooleanExpressions

const val myF = false
const val myT = true

fun test(someBoolean: Boolean) {
    val s = when (someBoolean) {
        <!CONFUSING_BRANCH_CONDITION_ERROR!>true || true<!> -> 1
        <!CONFUSING_BRANCH_CONDITION_ERROR!>false && false<!> -> 2
        true -> 3
        false -> 4
    }
}

fun test_2(someBoolean: Boolean) {
    val s = <!NO_ELSE_IN_WHEN!>when<!> (someBoolean) {
        <!CONFUSING_BRANCH_CONDITION_ERROR!>true || true<!> -> 1
        <!CONFUSING_BRANCH_CONDITION_ERROR!>false && false<!> -> 2
    }
}
