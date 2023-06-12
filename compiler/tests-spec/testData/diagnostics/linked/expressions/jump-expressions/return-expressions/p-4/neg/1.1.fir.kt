// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: expressions, jump-expressions, return-expressions -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: check returning is not allowed from run{...}
 */

// TESTCASE NUMBER: 1
// UNEXPECTED BEHAVIOUR
// ISSUES : KT-35545
fun <!IMPLICIT_NOTHING_RETURN_TYPE!>case1<!>(a: Boolean) = run { println("d"); return <!RETURN_TYPE_MISMATCH!>true<!> }

// TESTCASE NUMBER: 2
val <!IMPLICIT_NOTHING_PROPERTY_TYPE!>case2<!>
get() = run { println("d"); return <!RETURN_TYPE_MISMATCH!>true<!> }
