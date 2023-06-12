// !DIAGNOSTICS: -UNREACHABLE_CODE -IMPLICIT_CAST_TO_ANY -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-296
 * MAIN LINK: control--and-data-flow-analysis, control-flow-graph, expressions-1, conditional-expressions -> paragraph 1 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION: check if-expressions must have both branches. (attempt to pass Nothing to if-condition with 'else' key word)
 */

// TESTCASE NUMBER: 1

fun case1() {
    val y0else = <!INVALID_IF_AS_EXPRESSION!>if<!> (false) true else ;
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-35510
 */
fun case2(nothing: Nothing) {
    val n1else = <!INVALID_IF_AS_EXPRESSION!>if<!> (nothing) true else;
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-35510
 */
fun case3(nothing: Nothing) {
    val n1else = if (nothing) true else
<!SYNTAX!><!>}

// TESTCASE NUMBER: 4

fun case4(nothing: Nothing) {
    val x = if (false) else if (nothing) { "foo"} else
<!SYNTAX!><!>}

// TESTCASE NUMBER: 5

fun case5(nothing: Nothing) {
    val x = <!INVALID_IF_AS_EXPRESSION!>if<!> (false) else if (nothing) { "foo"} else ;
}
