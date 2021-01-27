// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30376
 */
fun case_1(x: Any?) {
    if ((x as Class).prop_8?.prop_8?.prop_8?.prop_8 != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & kotlin.Any?")!>x<!>.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & kotlin.Any?")!>x<!>.prop_8.<!UNSAFE_CALL!>prop_8<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & kotlin.Any?")!>x<!>.prop_8.<!UNSAFE_CALL!>prop_8<!>.<!UNSAFE_CALL!>prop_8<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & kotlin.Any?")!>x<!>.prop_8.<!UNSAFE_CALL!>prop_8<!>.<!UNSAFE_CALL!>prop_8<!>.<!UNSAFE_CALL!>prop_8<!>
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30376
 */
fun case_2(x: Class?) {
    if ((x as Class).prop_8?.prop_8?.prop_8?.prop_8 !== null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>.prop_8.<!UNSAFE_CALL!>prop_8<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>.prop_8.<!UNSAFE_CALL!>prop_8<!>.<!UNSAFE_CALL!>prop_8<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>.prop_8.<!UNSAFE_CALL!>prop_8<!>.<!UNSAFE_CALL!>prop_8<!>.<!UNSAFE_CALL!>prop_8<!>
    }
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30376
 */
fun case_3(x: Any?) {
    if ((x as Class?)?.prop_8?.prop_8?.prop_8?.prop_8 == null) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & kotlin.Any?")!>x<!>.<!UNSAFE_CALL!>prop_8<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & kotlin.Any?")!>x<!>.<!UNSAFE_CALL!>prop_8<!>.<!UNSAFE_CALL!>prop_8<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & kotlin.Any?")!>x<!>.<!UNSAFE_CALL!>prop_8<!>.<!UNSAFE_CALL!>prop_8<!>.<!UNSAFE_CALL!>prop_8<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & kotlin.Any?")!>x<!>.<!UNSAFE_CALL!>prop_8<!>.<!UNSAFE_CALL!>prop_8<!>.<!UNSAFE_CALL!>prop_8<!>.<!UNSAFE_CALL!>prop_8<!>.<!UNSAFE_CALL!>prop_8<!>
    }
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30376
 */
fun case_4(x: Any?) {
    if ((x as Class?)!!.prop_8?.prop_8?.prop_8?.prop_8 == null) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & kotlin.Any?")!>x<!>.<!UNSAFE_CALL!>prop_8<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & kotlin.Any?")!>x<!>.<!UNSAFE_CALL!>prop_8<!>.<!UNSAFE_CALL!>prop_8<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & kotlin.Any?")!>x<!>.<!UNSAFE_CALL!>prop_8<!>.<!UNSAFE_CALL!>prop_8<!>.<!UNSAFE_CALL!>prop_8<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & kotlin.Any?")!>x<!>.<!UNSAFE_CALL!>prop_8<!>.<!UNSAFE_CALL!>prop_8<!>.<!UNSAFE_CALL!>prop_8<!>.<!UNSAFE_CALL!>prop_8<!>.<!UNSAFE_CALL!>prop_8<!>
    }
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30376
 */
fun case_5(x: Class?) {
    if ((x?.prop_8 as Class).prop_8?.prop_8?.prop_8 == null == true) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>.prop_8.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>.prop_8.prop_8.<!UNSAFE_CALL!>prop_8<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>.prop_8.prop_8.<!UNSAFE_CALL!>prop_8<!>.<!UNSAFE_CALL!>prop_8<!>
    }
}

/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30376
 */
fun case_6(x: Class?) {
    if ((x?.prop_8?.prop_8?.prop_8 as Class?)?.prop_8 == null == true) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>.prop_8.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>.prop_8.prop_8.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>.prop_8.prop_8.prop_8.<!UNSAFE_CALL!>prop_8<!>.<!UNSAFE_CALL!>prop_8<!>
    }
}

// TESTCASE NUMBER: 5
fun <T> case_5(x: T) {
    if ((x as Any).propNullableT != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T")!>x<!>
    }
}

// TESTCASE NUMBER: 6
fun <T>case_6(x: Inv<T>?) {
    if (x?.prop_1?.prop_1?.prop_1?.prop_2 != null) {
        x.prop_1.prop_1.prop_1.prop_2
        x.prop_1.prop_1.prop_1.prop_2.equals(10)
    }
}

// TESTCASE NUMBER: 7
inline fun <reified T>case_7(x: Inv<T>?) {
    if (x?.prop_1?.prop_1?.prop_1?.prop_2 == null) else {
        x.prop_1.prop_1.prop_1.prop_2
        x.prop_1.prop_1.prop_1.prop_2.equals(10)
    }
}

// TESTCASE NUMBER: 8
fun <T>case_8(x: Inv<T>?) {
    if (x?.prop_1?.prop_1?.prop_1?.prop_1 == null) else {
        x.prop_1.prop_1.prop_1.prop_1
        x.prop_1.prop_1.prop_1.prop_1.equals(10)
    }
}

// TESTCASE NUMBER: 9
inline fun <reified T>case_9(x: Out<T>?) {
    if (x?.prop_1?.prop_1?.prop_1?.prop_1 != null) {
        x.prop_1.prop_1.prop_1.prop_1
        x.prop_1.prop_1.prop_1.prop_1.equals(10)
    }
}
