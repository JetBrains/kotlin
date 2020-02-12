// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 6
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1(x: Any?) {
    if (x is Int || x !is Int) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.<!DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inv<!>()
    }
}

// TESTCASE NUMBER: 2
fun case_2(x: Any) {
    if (x is Number || x !is Number || <!USELESS_IS_CHECK!>x is Number<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>()
    }
}

// TESTCASE NUMBER: 3
fun case_3(x: Any?) {
    if (x is Boolean || <!USELESS_IS_CHECK!>x !is Boolean is Boolean<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.<!UNRESOLVED_REFERENCE!>prop_1<!>
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: Any) {
    if (x !is EnumClass || <!USELESS_IS_CHECK!>x !is EnumClass<!> || <!USELESS_IS_CHECK!>x is EnumClass<!> || <!USELESS_IS_CHECK!>x is EnumClass<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.<!UNRESOLVED_REFERENCE!>fun_1<!>()
    }
}

// TESTCASE NUMBER: 5
fun case_5(x: Any?) {
    if (!(x !is Class.NestedClass?) || x is Class.NestedClass? || x !is Class.NestedClass?) {
        if (!!(x !is Class.NestedClass?)) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.<!UNRESOLVED_REFERENCE!>prop_4<!>
        }
    }
}

// TESTCASE NUMBER: 6
fun case_6(x: Any?) {
    if (!(x is Object) || !!(<!USELESS_IS_CHECK!>x !is Object<!>)) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.<!UNRESOLVED_REFERENCE!>prop_1<!>
    }
}

// TESTCASE NUMBER: 7
fun case_7(x: Any) {
    if (!(x is DeepObject.A.B.C.D.E.F.G.J) || !!!!!!(<!USELESS_IS_CHECK!>x is DeepObject.A.B.C.D.E.F.G.J<!>)) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.<!UNRESOLVED_REFERENCE!>prop_1<!>
    }
}

// TESTCASE NUMBER: 8
fun case_8(x: Any?) {
    if (x is Int? == x is Int?) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>?.<!DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inv<!>()
    }
}

// TESTCASE NUMBER: 9
fun case_9(x: Any?) {
    if (!!!(x !is TypealiasNullableStringIndirect<!REDUNDANT_NULLABLE!>?<!>)) else {
        if (!(x !is TypealiasNullableStringIndirect<!REDUNDANT_NULLABLE!>?<!>)) else {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>?.<!DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>get<!>(0)
        }
    }
}

// TESTCASE NUMBER: 10
fun case_10(x: Any?) {
    if (!!(x !is Interface3)) {
        if (!!!(x is Interface3)) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.<!UNRESOLVED_REFERENCE!>itest<!>()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.<!UNRESOLVED_REFERENCE!>itest3<!>()
        }
    }
}

// TESTCASE NUMBER: 11
fun case_11(x: Any?) {
    if (x is SealedMixedChildObject1?) else {
        if (x is SealedMixedChildObject1?) else {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>?.<!UNRESOLVED_REFERENCE!>prop_1<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>?.<!UNRESOLVED_REFERENCE!>prop_2<!>
        }
    }
}

// TESTCASE NUMBER: 12
inline fun <reified T, reified K>case_12(x: Any?) {
    if (x !is T) {
        if (x is T is K) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
        }
    }
}

// TESTCASE NUMBER: 13
inline fun <reified T, reified K>case_13(x: Any?) {
    if (x !is T) {
        if (x !is K) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
        }
    }
}

// TESTCASE NUMBER: 14
inline fun <reified T, reified K>case_14(x: Any?) {
    if (x is K) else {
        if (x !is T) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
        }
    }
}

// TESTCASE NUMBER: 15
inline fun <reified T, reified K>case_15(x: Any?) {
    if (x !is T) {
        if (x is K) else {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
        }
    }
}
