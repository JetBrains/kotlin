// WITH_STDLIB
// ISSUE: KT-56564

sealed interface A
sealed interface B

class SubA: A
class SubB: B
class SubAandB: A, B

fun test(a: A): Int {
    var i = 0
    i += <!NO_ELSE_IN_WHEN!>when<!>(a) {
        is SubAandB -> 1
    }
    i += <!NO_ELSE_IN_WHEN!>when<!>(a) {
        <!USELESS_IS_CHECK!>is SubB<!> -> 1
        is SubAandB -> 1
    }
    i += when(a) {
        is SubA -> 1
        is SubAandB -> 1
    }
    i += when(a) {
        <!USELESS_IS_CHECK!>is A<!> -> 1
        is SubA -> 1
        is B -> 1
        <!USELESS_IS_CHECK!>is SubB<!> -> 1
        is SubAandB -> 1
    }
    return i
}

fun test(b: B): Int {
    var i = 0
    i += <!NO_ELSE_IN_WHEN!>when<!>(b) {
        is SubAandB -> 1
    }
    i += when(b) {
        is SubB -> 1
        is SubAandB -> 1
    }
    i += <!NO_ELSE_IN_WHEN!>when<!>(b) {
        <!USELESS_IS_CHECK!>is SubA<!> -> 1
        is SubAandB -> 1
    }
    i += when(b) {
        is A -> 1
        <!USELESS_IS_CHECK!>is SubA<!> -> 1
        <!USELESS_IS_CHECK!>is B<!> -> 1
        is SubB -> 1
        is SubAandB -> 1
    }
    return i
}

fun testIntersection(both: A): Int {
    check(both is B)

    var i = 0
    i += <!NO_ELSE_IN_WHEN!>when<!>(both) {
        is SubAandB -> 1
    }
    i += when(both) {
        <!USELESS_IS_CHECK!>is SubB<!> -> 1
        is SubAandB -> 1
    }
    i += when(both) {
        <!USELESS_IS_CHECK!>is SubA<!> -> 1
        is SubAandB -> 1
    }
    i += when(both) {
        <!USELESS_IS_CHECK!>is A<!> -> 1
        <!USELESS_IS_CHECK!>is SubA<!> -> 1
        <!USELESS_IS_CHECK!>is B<!> -> 1
        <!USELESS_IS_CHECK!>is SubB<!> -> 1
        is SubAandB -> 1
    }
    return i
}

fun testIntersection(both: B): Int {
    check(both is A)

    var i = 0
    i += <!NO_ELSE_IN_WHEN!>when<!>(both) {
        is SubAandB -> 1
    }
    i += when(both) {
        <!USELESS_IS_CHECK!>is SubB<!> -> 1
        is SubAandB -> 1
    }
    i += when(both) {
        <!USELESS_IS_CHECK!>is SubA<!> -> 1
        is SubAandB -> 1
    }
    i += when(both) {
        <!USELESS_IS_CHECK!>is A<!> -> 1
        <!USELESS_IS_CHECK!>is SubA<!> -> 1
        <!USELESS_IS_CHECK!>is B<!> -> 1
        <!USELESS_IS_CHECK!>is SubB<!> -> 1
        is SubAandB -> 1
    }
    return i
}
