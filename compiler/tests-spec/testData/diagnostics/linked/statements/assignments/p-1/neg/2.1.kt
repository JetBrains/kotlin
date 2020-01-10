// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-222
 * PLACE: statements, assignments -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: Both left-hand and right-hand sides of an assignment must be expressions
 */


/*
 * TESTCASE NUMBER: 1
 * NOTE: right-hand side of an assignment must be expression
 */
fun case1() {
    val x = <!EXPRESSION_EXPECTED!>for (<!SYNTAX!><!>) { }<!>
    val y = <!EXPRESSION_EXPECTED!>for (<!NAME_SHADOWING!>x<!> in 1..2) { }<!>

    val a = <!EXPRESSION_EXPECTED!>while (<!SYNTAX!><!>) { }<!>
    val b = <!EXPRESSION_EXPECTED!>while (false) { }<!>
    val c = <!EXPRESSION_EXPECTED!>while (<!SYNTAX!><!>) <!>;
}

/*
 * TESTCASE NUMBER: 2
 * NOTE: right-hand side of an assignment must be expression
 */
fun case2() {
    var x = <!EXPRESSION_EXPECTED!>for (<!SYNTAX!><!>) { }<!>
    var y = <!EXPRESSION_EXPECTED!>for (<!NAME_SHADOWING!>x<!> in 1..2) { }<!>

    var a = <!EXPRESSION_EXPECTED!>while (<!SYNTAX!><!>) { }<!>
    var b = <!EXPRESSION_EXPECTED!>while (false) { }<!>
    var c = <!EXPRESSION_EXPECTED!>while (<!SYNTAX!><!>) <!>;
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

    x = <!EXPRESSION_EXPECTED!>for (<!SYNTAX!><!>) { }<!>
    y = <!EXPRESSION_EXPECTED!>for (<!NAME_SHADOWING!>x<!> in 1..2) { }<!>

    a = <!EXPRESSION_EXPECTED!>while (<!SYNTAX!><!>) { }<!>
    b = <!EXPRESSION_EXPECTED!>while (false) { }<!>
    c = <!EXPRESSION_EXPECTED!>while (<!SYNTAX!><!>) <!>;
}

/*
 * TESTCASE NUMBER: 4
 * NOTE: left-hand side of an assignment must be expression
 */
fun case4() {
    <!EXPRESSION_EXPECTED!>for (x in 1..2) {}<!>  <!UNREACHABLE_CODE!>=<!> TODO();

    <!UNREACHABLE_CODE!><!EXPRESSION_EXPECTED!>while (false) { }<!> = TODO()<!>
}