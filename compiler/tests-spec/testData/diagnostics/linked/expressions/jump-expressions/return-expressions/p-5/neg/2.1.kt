// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, jump-expressions, return-expressions -> paragraph 5 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: check returning is not allowed from run{...}
 */

// TESTCASE NUMBER: 1
// UNEXPECTED BEHAVIOUR
// ISSUES : KT-35545
fun <!IMPLICIT_NOTHING_RETURN_TYPE!>case1<!>(a: Boolean) = run { println("d"); return <!TYPE_MISMATCH!>true<!> }

// TESTCASE NUMBER: 2
val <!IMPLICIT_NOTHING_PROPERTY_TYPE!>case2<!>
get() = run { println("d"); return <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>true<!> }

