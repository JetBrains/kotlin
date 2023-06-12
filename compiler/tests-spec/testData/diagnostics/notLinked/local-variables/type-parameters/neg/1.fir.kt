// !DIAGNOSTICS: -UNUSED_VARIABLE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: local-variables, type-parameters
 * NUMBER: 1
 * DESCRIPTION: Local variables with forbidden type parameters.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-8341
 */

// TESTCASE NUMBER: 1
fun case_1() {
    val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T><!> x1 = 1
    var <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T><!> x2 = 1
}

// TESTCASE NUMBER: 2
fun case_2() {
    val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T><!> x1: Int = 1
    var <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T><!> x2: Int = 1
}

// TESTCASE NUMBER: 3
fun case_3() {
    val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T><!> x1: Map<Int, Int> = mapOf(1 to 1)
    var <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T><!> x2: Map<Int, Int> = mapOf(1 to 1)
}

// TESTCASE NUMBER: 4
fun case_4() {
    val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T><!> y1: Number where __UNRESOLVED__: __UNRESOLVED__ = 1
    var <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T><!> y2: Number where __UNRESOLVED__: __UNRESOLVED__ = 1
}

// TESTCASE NUMBER: 5
fun case_5() {
    val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T : <!UNRESOLVED_REFERENCE!>__UNRESOLVED__<!>><!> x1: Map<Int, Int> = mapOf(1 to 1)
    var <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T : <!UNRESOLVED_REFERENCE!>__UNRESOLVED__<!>><!> x2: Map<Int, Int> = mapOf(1 to 1)
}

/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 */
fun case_6() {
    val <T : __UNRESOLVED__> (x1, y1) = Pair(1, 2)
    var <T : __UNRESOLVED__> (x2, y2) = Pair(1, 2)
}

/*
 * TESTCASE NUMBER: 7
 * UNEXPECTED BEHAVIOUR
 */
fun case_7() {
    val <T : __UNRESOLVED__> (x1, y1) where __UNRESOLVED__: __UNRESOLVED__ = Pair(1, 2)
    var <T : __UNRESOLVED__> (x2, y2) where __UNRESOLVED__: __UNRESOLVED__ = Pair(1, 2)
}

// TESTCASE NUMBER: 8
fun case_8() {
    val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><A, B : A, C : B, D : C, E : D><!> x1 = 1
    var <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><A, B : A, C : B, D : C, E : D><!> x2 = 2
}

// TESTCASE NUMBER: 9
fun case_9(y: Boolean?) = when (val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T><!> x = y) {
    true -> null
    false -> null
    null -> null
}

// TESTCASE NUMBER: 10
fun case_10(x: Boolean?) = when (val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T><!> x where T: suspend () -> Unit, T: Boolean = x) {
    true -> null
    false -> null
    null -> null
}

// TESTCASE NUMBER: 11
fun case_11() {
    val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T><!> <!REDECLARATION!>x<!> by lazy { 1 }
    var <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T><!> <!REDECLARATION!>x<!> by <!DELEGATE_SPECIAL_FUNCTION_MISSING!>lazy { 1 }<!>
}

// TESTCASE NUMBER: 12
fun case_12() {
    val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T : <!UNRESOLVED_REFERENCE!>__UNRESOLVED__<!>><!> <!REDECLARATION!>x<!>: Int
    var <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T : <!UNRESOLVED_REFERENCE!>__UNRESOLVED__<!>><!> <!REDECLARATION!>x<!>: Int
}

// TESTCASE NUMBER: 13
fun case_13() {
    val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T : <!UNRESOLVED_REFERENCE!>__UNRESOLVED__<!>><!> <!REDECLARATION!>x<!>: Int where __UNRESOLVED__: __UNRESOLVED__
    var <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><T : <!UNRESOLVED_REFERENCE!>__UNRESOLVED__<!>><!> <!REDECLARATION!>x<!>: Int where __UNRESOLVED__: __UNRESOLVED__
}

// TESTCASE NUMBER: 14
fun case_14() {
    val <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><<!CYCLIC_GENERIC_UPPER_BOUND!>T : T<!>><!> x1 = 1
    var <!LOCAL_VARIABLE_WITH_TYPE_PARAMETERS!><<!CYCLIC_GENERIC_UPPER_BOUND!>T : T<!>><!> x2 = 1
}
