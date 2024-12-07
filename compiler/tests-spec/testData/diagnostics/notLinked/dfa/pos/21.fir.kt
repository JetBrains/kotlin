// DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT
// WITH_EXTRA_CHECKERS

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 21
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1(x: Any?) {
    if (x is Int == true) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x<!>.inv()
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28329
 */
fun case_2(x: Any) {
    if (<!DEPRECATED_IDENTITY_EQUALS!>x is Int === true<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x<!>.inv()
    }
}

// TESTCASE NUMBER: 3
fun case_3(x: Any?) {
    if (x !is Class == true == true == true == true == true) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>x<!>.prop_1
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: Any) {
    if (x !is EnumClass != false) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("EnumClass")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("EnumClass")!>x<!>.fun_1()
    }
}

// TESTCASE NUMBER: 5
fun case_5(x: Any?) {
    if (!(x !is Class.NestedClass?) != false == true) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class.NestedClass?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class.NestedClass?")!>x<!>?.prop_4
    }
}

// TESTCASE NUMBER: 6
fun case_6(x: Any?) {
    if (!(x !is Object) != false != false != false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Object")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Object")!>x<!>.prop_1
    }
}

/*
 * TESTCASE NUMBER: 7
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28329
 */
fun case_7(x: Any) {
    if (<!DEPRECATED_IDENTITY_EQUALS!>!(x is DeepObject.A.B.C.D.E.F.G.J) !== false<!>) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>x<!>.prop_1
    }
}

/*
 * TESTCASE NUMBER: 8
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28329
 */
fun case_8(x: Any?) {
    if (<!DEPRECATED_IDENTITY_EQUALS!><!DEPRECATED_IDENTITY_EQUALS!><!DEPRECATED_IDENTITY_EQUALS!>!(x is Int?) !== false<!> !== false<!> !== false<!>) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!>?.inv()
    }
}

/*
 * TESTCASE NUMBER: 9
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28329
 */
fun case_9(x: Any?) {
    if (<!DEPRECATED_IDENTITY_EQUALS!><!DEPRECATED_IDENTITY_EQUALS!>!!(x !is TypealiasNullableStringIndirect<!REDUNDANT_NULLABLE!>?<!>) !== false<!> === true<!>) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x<!>?.get(0)
    }
}

/*
 * TESTCASE NUMBER: 10
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28329
 */
fun case_10(x: Any?) {
    if (<!DEPRECATED_IDENTITY_EQUALS!>!!(x !is Interface3) === true<!> && true) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3")!>x<!>.itest()
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3")!>x<!>.itest3()
    }
}

// TESTCASE NUMBER: 11
fun case_11(x: Any?) {
    if (x is SealedMixedChildObject1? != false || false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("SealedMixedChildObject1?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("SealedMixedChildObject1?")!>x<!>?.prop_1
        <!DEBUG_INFO_EXPRESSION_TYPE("SealedMixedChildObject1?")!>x<!>?.prop_2
    }
}
