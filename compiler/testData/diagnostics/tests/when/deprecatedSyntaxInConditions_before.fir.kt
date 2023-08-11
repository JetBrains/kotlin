// LANGUAGE: -ProhibitConfusingSyntaxInWhenBranches
// DIAGNOSTICS: -INCOMPATIBLE_TYPES, -NON_EXHAUSTIVE_WHEN_STATEMENT, -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
// ISSUE: KT-48385

interface A {
    operator fun plus(other: A): A
    operator fun minus(other: A): A
    operator fun times(other: A): A
    operator fun div(other: A): A
    operator fun rem(other: A): A

    operator fun rangeTo(other: A): A

    operator fun compareTo(other: A): Int

    fun id(): A
}

interface B {
    operator fun inc(): B
    operator fun dec(): B

    operator fun plusAssign(other: B)
    operator fun minusAssign(other: B)
    operator fun timesAssign(other: B)
    operator fun divAssign(other: B)
    operator fun remAssign(other: B)
}

operator fun Any?.contains(other: Any): Boolean = false

fun testWithSubject_ok(x: A, y: A?, any: Any, z: B) {
   when (x) {
       x.id() -> {}
       y?.id() -> {}
       any as? A -> {}
       any as A -> {}
       x * x -> {}
       x / x -> {}
       x % x -> {}
       x + x -> {}
       x - x -> {}
       x..x -> {}
       y -> {}
       y ?: x -> {}
   }

    var b = z
    when (z) {
        b++ -> {}
        b-- -> {}
    }
}

fun testWithSubject_bad_1(x: A) {
    // bad
    when (x) {
        <!CONFUSING_BRANCH_CONDITION_WARNING!>x in x<!> -> {}
        <!CONFUSING_BRANCH_CONDITION_WARNING!>x !in x<!> -> {}
        <!CONFUSING_BRANCH_CONDITION_WARNING!>x is String<!> -> {}
        <!CONFUSING_BRANCH_CONDITION_WARNING!>x !is String<!> -> {}
        <!CONFUSING_BRANCH_CONDITION_WARNING!>x < x<!> -> {}
        <!CONFUSING_BRANCH_CONDITION_WARNING!>x > x<!> -> {}
        <!CONFUSING_BRANCH_CONDITION_WARNING!>x <= x<!> -> {}
        <!CONFUSING_BRANCH_CONDITION_WARNING!>x >= x<!> -> {}
        <!CONFUSING_BRANCH_CONDITION_WARNING!>x == x<!> -> {}
        <!CONFUSING_BRANCH_CONDITION_WARNING!>x != x<!> -> {}
        <!CONFUSING_BRANCH_CONDITION_WARNING!>x === x<!> -> {}
        <!CONFUSING_BRANCH_CONDITION_WARNING!>x !== x<!> -> {}
    }
    // ok
    when (x) {
        (x in x) -> {}
        (x !in x) -> {}
        (x is String) -> {}
        (x !is String) -> {}
        (x < x) -> {}
        (x > x) -> {}
        (x <= x) -> {}
        (x >= x) -> {}
        (x == x) -> {}
        (x != x) -> {}
        (x === x) -> {}
        (x !== x) -> {}
    }
}

fun testWithSubject_bad_2(b: Boolean) {
    // bad
    <!NO_ELSE_IN_WHEN!>when<!> (b) {
        <!CONFUSING_BRANCH_CONDITION_WARNING!>b && b<!> -> {}
        <!CONFUSING_BRANCH_CONDITION_WARNING!>b || b<!> -> {}
    }
    // ok
    <!NO_ELSE_IN_WHEN!>when<!> (b) {
        (b && b) -> {}
        (b || b) -> {}
    }
}

fun testWithSubject_bad_3(a: Array<Any>) {
    // bad
    when (a) {
        <!SYNTAX!><!SYNTAX!><!>*<!>a <!SYNTAX!><!>-> {}
    }
    // also bad
    when (a) {
        (<!UNRESOLVED_REFERENCE!><!SYNTAX!><!>*<!>a<!SYNTAX!>)<!><!SYNTAX!><!> <!SYNTAX!><!>-> {}
    }
}

fun testWithSubject_bad_4(b: B) {
    var x = b
    // bad
    when (b) {
        <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>x = b<!> -> {}
        <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>b += b<!> -> {}
        <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>b -= b<!> -> {}
        <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>b *= b<!> -> {}
        <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>b /= b<!> -> {}
        <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>b %= b<!> -> {}
    }
    // also bad
    when (b) {
        (<!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>x = b<!>) -> {}
        (<!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>b += b<!>) -> {}
        (<!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>b -= b<!>) -> {}
        (<!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>b *= b<!>) -> {}
        (<!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>b /= b<!>) -> {}
        (<!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>b %= b<!>) -> {}
    }
}

// range

fun testWithRange_ok(x: A, y: A?, any: Any, z: B) {
    when (x) {
        in x.id() -> {}
        in y?.id() -> {}
        in any as? A -> {}
        in any as A -> {}
        in x * x -> {}
        in x / x -> {}
        in x % x -> {}
        in x + x -> {}
        in x - x -> {}
        in x..x -> {}
        in y -> {}
        in y ?: x -> {}
    }

    var b = z
    when (z) {
        in b++ -> {}
        in b-- -> {}
    }
}

fun testWithRange_bad_1(x: A) {
    // bad
    when (x) {
        in <!CONFUSING_BRANCH_CONDITION_WARNING!>x in x<!> -> {}
        in <!CONFUSING_BRANCH_CONDITION_WARNING!>x !in x<!> -> {}
        in <!CONFUSING_BRANCH_CONDITION_WARNING!>x is String<!> -> {}
        in <!CONFUSING_BRANCH_CONDITION_WARNING!>x !is String<!> -> {}
        in <!CONFUSING_BRANCH_CONDITION_WARNING!>x < x<!> -> {}
        in <!CONFUSING_BRANCH_CONDITION_WARNING!>x > x<!> -> {}
        in <!CONFUSING_BRANCH_CONDITION_WARNING!>x <= x<!> -> {}
        in <!CONFUSING_BRANCH_CONDITION_WARNING!>x >= x<!> -> {}
        in <!CONFUSING_BRANCH_CONDITION_WARNING!>x == x<!> -> {}
        in <!CONFUSING_BRANCH_CONDITION_WARNING!>x != x<!> -> {}
        in <!CONFUSING_BRANCH_CONDITION_WARNING!>x === x<!> -> {}
        in <!CONFUSING_BRANCH_CONDITION_WARNING!>x !== x<!> -> {}
    }
    // ok
    when (x) {
        in (x in x) -> {}
        in (x !in x) -> {}
        in (x is String) -> {}
        in (x !is String) -> {}
        in (x < x) -> {}
        in (x > x) -> {}
        in (x <= x) -> {}
        in (x >= x) -> {}
        in (x == x) -> {}
        in (x != x) -> {}
        in (x === x) -> {}
        in (x !== x) -> {}
    }
}

fun testWithRange_bad_2(b: Boolean) {
    // bad
    <!NO_ELSE_IN_WHEN!>when<!> (b) {
        in <!CONFUSING_BRANCH_CONDITION_WARNING!>b && b<!> -> {}
        in <!CONFUSING_BRANCH_CONDITION_WARNING!>b || b<!> -> {}
    }
    // ok
    <!NO_ELSE_IN_WHEN!>when<!> (b) {
        in (b && b) -> {}
        in (b || b) -> {}
    }
}

fun testWithRange_bad_3(a: Array<Any>) {
    // bad
    when (a) {
        in<!SYNTAX!><!> <!SYNTAX!>*<!>a <!SYNTAX!><!>-> {}
    }
    // also bad
    when (a) {
        in (<!UNRESOLVED_REFERENCE!><!SYNTAX!><!>*<!>a<!SYNTAX!>)<!><!SYNTAX!><!> <!SYNTAX!><!>-> {}
    }
}

fun testWithRange_bad_4(b: B) {
    var x = b
    // bad
    when (b) {
        in <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>x = b<!> -> {}
        in <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>b += b<!> -> {}
        in <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>b -= b<!> -> {}
        in <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>b *= b<!> -> {}
        in <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>b /= b<!> -> {}
        in <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>b %= b<!> -> {}
    }
    // also bad
    when (b) {
        in (<!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>x = b<!>) -> {}
        in (<!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>b += b<!>) -> {}
        in (<!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>b -= b<!>) -> {}
        in (<!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>b *= b<!>) -> {}
        in (<!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>b /= b<!>) -> {}
        in (<!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>b %= b<!>) -> {}
    }
}
