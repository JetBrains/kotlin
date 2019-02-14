// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 3
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1(x: Nothing?) {
    if (<!DEBUG_INFO_CONSTANT!>x<!> is Int) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?"), SMARTCAST_IMPOSSIBLE!>x<!>.inv()
    }
}

// TESTCASE NUMBER: 2
fun case_2(x: Nothing) {
    if (<!USELESS_IS_CHECK!>x <!UNREACHABLE_CODE!>is Unit<!><!>) <!UNREACHABLE_CODE!>{
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>.<!MISSING_DEPENDENCY_CLASS, MISSING_DEPENDENCY_CLASS!>inv<!>()
    }<!>
}

// TESTCASE NUMBER: 3
fun case_3(x: Nothing?) {
    if (<!DEBUG_INFO_CONSTANT!>x<!> !is Class) else {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?"), SMARTCAST_IMPOSSIBLE!>x<!>.prop_1
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: Nothing) {
    if (<!USELESS_IS_CHECK!>x <!UNREACHABLE_CODE!>!is EnumClass<!><!>) else <!UNREACHABLE_CODE!>{
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>.<!UNRESOLVED_REFERENCE!>fun_1<!>()
    }<!>
}

// TESTCASE NUMBER: 5
fun case_5(x: Nothing?) {
    if (!(<!USELESS_IS_CHECK!><!DEBUG_INFO_CONSTANT!>x<!> !is Class.NestedClass?<!>)) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>?.<!UNRESOLVED_REFERENCE!>prop_4<!>
    }
}

// TESTCASE NUMBER: 6
fun case_6(x: Nothing?) {
    if (!(<!DEBUG_INFO_CONSTANT!>x<!> !is Object)) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?"), SMARTCAST_IMPOSSIBLE!>x<!>.prop_1
    }
}

// TESTCASE NUMBER: 7
fun case_7(x: Nothing) {
    if (<!UNREACHABLE_CODE!>!(<!><!USELESS_IS_CHECK!>x is DeepObject.A.B.C.D.E.F.G.J<!><!UNREACHABLE_CODE!>)<!>) else <!UNREACHABLE_CODE!>{
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>.<!UNRESOLVED_REFERENCE!>prop_1<!>
    }<!>
}

// TESTCASE NUMBER: 8
fun case_8(x: Nothing?) {
    if (!(<!USELESS_IS_CHECK!><!DEBUG_INFO_CONSTANT!>x<!> is Int?<!>)) else {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>?.<!MISSING_DEPENDENCY_CLASS, MISSING_DEPENDENCY_CLASS!>inv<!>()
    }
}

// TESTCASE NUMBER: 9
fun case_9(x: Nothing?) {
    if (!!(<!USELESS_IS_CHECK!><!DEBUG_INFO_CONSTANT!>x<!> !is TypealiasNullableStringIndirect<!REDUNDANT_NULLABLE!>?<!><!>)) else {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>?.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

// TESTCASE NUMBER: 10
fun case_10(x: Nothing?) {
    if (!!(<!DEBUG_INFO_CONSTANT!>x<!> !is Interface3)) else {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?"), SMARTCAST_IMPOSSIBLE!>x<!>.itest()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?"), SMARTCAST_IMPOSSIBLE!>x<!>.itest3()
    }
}

// TESTCASE NUMBER: 11
fun case_11(x: Nothing?) {
    if (<!USELESS_IS_CHECK!><!DEBUG_INFO_CONSTANT!>x<!> is SealedMixedChildObject1?<!>) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>?.<!UNRESOLVED_REFERENCE!>prop_1<!>
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>?.<!UNRESOLVED_REFERENCE!>prop_2<!>
    }
}
