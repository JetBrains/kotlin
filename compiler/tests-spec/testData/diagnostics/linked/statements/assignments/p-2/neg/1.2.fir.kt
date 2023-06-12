// !DIAGNOSTICS: -UNSAFE_CALL -UNREACHABLE_CODE -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-222
 * MAIN LINK: statements, assignments -> paragraph 2 -> sentence 1
 * PRIMARY LINKS: statements, assignments -> paragraph 3 -> sentence 2
 * statements, assignments, simple-assignments -> paragraph 1 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: Check the expression is not assignable if a navigation expression referring to an unmutable property
 */


/*
 * TESTCASE NUMBER: 1
 * NOTE: a navigation expression referring to a unmutable property
 */
fun case1() {
    val x : Case1? = Case1()
    x.<!VAL_REASSIGNMENT!>x<!> = "0"
    x?.<!VAL_REASSIGNMENT!>x<!> = "0"
    <!VARIABLE_EXPECTED!>x::<!UNRESOLVED_REFERENCE!>x<!><!> = TODO()
}

class Case1{
    val x : Any?
        get() { TODO() }
}

/*
 * TESTCASE NUMBER: 2
 * NOTE: an identifier referring to a ununmutable property
 */
fun case2() {
    val x : Case2? = Case2(null)
    x.<!VAL_REASSIGNMENT!>x<!> = "0"
    x?.<!VAL_REASSIGNMENT!>x<!> = "0"
    <!VARIABLE_EXPECTED!>x::<!UNRESOLVED_REFERENCE!>x<!><!> = TODO()
}

class Case2(val x: Any?) {}

/*
 * TESTCASE NUMBER: 3
 * NOTE: an identifier referring to a ununmutable property
 */
fun case3() {
    val x : Case3? = Case3()
    x.<!VAL_REASSIGNMENT!>x<!> = "0"
    x?.<!VAL_REASSIGNMENT!>x<!> = "0"
    <!VARIABLE_EXPECTED!>x::<!UNRESOLVED_REFERENCE!>x<!><!> = TODO()
}

class Case3() {
    val x: Any? = null
}
