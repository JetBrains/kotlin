// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(x: Nothing?) {
    if (x is Int) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.Nothing")!>x<!>.inv()
    }
}

// TESTCASE NUMBER: 2
fun case_2(x: Nothing) {
    if (x is Unit) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>.inv()
    }
}

// TESTCASE NUMBER: 3
fun case_3(x: Nothing?) {
    if (x !is Class) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.Nothing")!>x<!>.<!UNRESOLVED_REFERENCE!>prop_1<!>
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: Nothing) {
    if (x !is EnumClass) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>.<!UNRESOLVED_REFERENCE!>fun_1<!>()
    }
}

// TESTCASE NUMBER: 5
fun case_5(x: Nothing?) {
    if (!(x !is Class.NestedClass?)) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>?.<!UNRESOLVED_REFERENCE!>prop_4<!>
    }
}

// TESTCASE NUMBER: 6
fun case_6(x: Nothing?) {
    if (!(x !is Object)) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.Nothing")!>x<!>.<!UNRESOLVED_REFERENCE!>prop_1<!>
    }
}

// TESTCASE NUMBER: 7
fun case_7(x: Nothing) {
    if (!(x is DeepObject.A.B.C.D.E.F.G.J)) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>.<!UNRESOLVED_REFERENCE!>prop_1<!>
    }
}

// TESTCASE NUMBER: 8
fun case_8(x: Nothing?) {
    if (!(x is Int?)) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>?.inv()
    }
}

// TESTCASE NUMBER: 9
fun case_9(x: Nothing?) {
    if (!!(x !is TypealiasNullableStringIndirect?)) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>?.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

// TESTCASE NUMBER: 10
fun case_10(x: Nothing?) {
    if (!!(x !is Interface3)) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.Nothing")!>x<!>.<!UNRESOLVED_REFERENCE!>itest<!>()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.Nothing")!>x<!>.<!UNRESOLVED_REFERENCE!>itest3<!>()
    }
}

// TESTCASE NUMBER: 11
fun case_11(x: Nothing?) {
    if (x is SealedMixedChildObject1?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>?.<!UNRESOLVED_REFERENCE!>prop_1<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>?.<!UNRESOLVED_REFERENCE!>prop_2<!>
    }
}
