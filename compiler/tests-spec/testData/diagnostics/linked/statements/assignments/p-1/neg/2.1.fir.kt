// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * NOTE: right-hand side of an assignment must be expression
 */
fun case1() {
    val x = <!OVERLOAD_RESOLUTION_AMBIGUITY, UNRESOLVED_REFERENCE!>for (<!SYNTAX!><!>) { }<!>
    val y = for (x in 1..2) { }

    val a = <!EXPRESSION_REQUIRED!>while (<!SYNTAX!><!>) { }<!>
    val b = <!EXPRESSION_REQUIRED!>while (false) { }<!>
    val c = <!EXPRESSION_REQUIRED!>while (<!SYNTAX!><!>) <!>;
}

/*
 * TESTCASE NUMBER: 2
 * NOTE: right-hand side of an assignment must be expression
 */
fun case2() {
    var x = <!OVERLOAD_RESOLUTION_AMBIGUITY, UNRESOLVED_REFERENCE!>for (<!SYNTAX!><!>) { }<!>
    var y = for (x in 1..2) { }

    var a = <!EXPRESSION_REQUIRED!>while (<!SYNTAX!><!>) { }<!>
    var b = <!EXPRESSION_REQUIRED!>while (false) { }<!>
    var c = <!EXPRESSION_REQUIRED!>while (<!SYNTAX!><!>) <!>;
}

/*
 * TESTCASE NUMBER: 3
 * NOTE: right-hand side of an assignment must be expression
 */
fun case3() {
    var x :Any?
    var y :Any?

    var a :Any?
    var b :Any?
    var c :Any?

    x = <!OVERLOAD_RESOLUTION_AMBIGUITY, UNRESOLVED_REFERENCE!>for (<!SYNTAX!><!>) { }<!>
    y = for (x in 1..2) { }

    a = <!EXPRESSION_REQUIRED!>while (<!SYNTAX!><!>) { }<!>
    b = <!EXPRESSION_REQUIRED!>while (false) { }<!>
    c = <!EXPRESSION_REQUIRED!>while (<!SYNTAX!><!>) <!>;
}

/*
 * TESTCASE NUMBER: 4
 * NOTE: left-hand side of an assignment must be expression
 */
fun case4() {
    <!VARIABLE_EXPECTED!>for (x in 1..2) {}<!>  = TODO();

    <!VARIABLE_EXPECTED!>while (false) { }<!> = TODO()
}
