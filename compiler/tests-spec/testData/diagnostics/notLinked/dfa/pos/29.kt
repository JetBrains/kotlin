// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 29
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, properties, objects, typealiases, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1(x: Class?) {
    if (x?.prop_8?.prop_8?.prop_8?.prop_8 != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!><!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.prop_8<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!><!DEBUG_INFO_EXPRESSION_TYPE("Class"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.prop_8<!>.prop_8<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!><!DEBUG_INFO_EXPRESSION_TYPE("Class"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_EXPRESSION_TYPE("Class"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.prop_8<!>.prop_8<!>.prop_8<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!><!DEBUG_INFO_EXPRESSION_TYPE("Class"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_EXPRESSION_TYPE("Class"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_EXPRESSION_TYPE("Class"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.prop_8<!>.prop_8<!>.prop_8<!>.prop_8<!>
    }
}

// TESTCASE NUMBER: 2
fun case_2(x: Class?) {
    if (x?.prop_8?.prop_8?.prop_8?.prop_8 !== null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!><!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.prop_8<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!><!DEBUG_INFO_EXPRESSION_TYPE("Class"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.prop_8<!>.prop_8<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!><!DEBUG_INFO_EXPRESSION_TYPE("Class"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_EXPRESSION_TYPE("Class"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.prop_8<!>.prop_8<!>.prop_8<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!><!DEBUG_INFO_EXPRESSION_TYPE("Class"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_EXPRESSION_TYPE("Class"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_EXPRESSION_TYPE("Class"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.prop_8<!>.prop_8<!>.prop_8<!>.prop_8<!>
    }
}

// TESTCASE NUMBER: 3
fun case_3(x: Class?) {
    if (x?.prop_8?.prop_8?.prop_8?.prop_8 == null) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!><!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.prop_8<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!><!DEBUG_INFO_EXPRESSION_TYPE("Class"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.prop_8<!>.prop_8<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!><!DEBUG_INFO_EXPRESSION_TYPE("Class"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_EXPRESSION_TYPE("Class"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.prop_8<!>.prop_8<!>.prop_8<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!><!DEBUG_INFO_EXPRESSION_TYPE("Class"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_EXPRESSION_TYPE("Class"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_EXPRESSION_TYPE("Class"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.prop_8<!>.prop_8<!>.prop_8<!>.prop_8<!>
    }
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30369
 */
fun case_4(x: Class?) {
    if (x?.prop_8?.prop_8?.prop_8?.prop_8 == null == true) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class?")!><!DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!><!UNSAFE_CALL!>.<!>prop_8<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class?")!><!DEBUG_INFO_EXPRESSION_TYPE("Class?")!><!DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!><!UNSAFE_CALL!>.<!>prop_8<!><!UNSAFE_CALL!>.<!>prop_8<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class?")!><!DEBUG_INFO_EXPRESSION_TYPE("Class?")!><!DEBUG_INFO_EXPRESSION_TYPE("Class?")!><!DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!><!UNSAFE_CALL!>.<!>prop_8<!><!UNSAFE_CALL!>.<!>prop_8<!><!UNSAFE_CALL!>.<!>prop_8<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class?")!><!DEBUG_INFO_EXPRESSION_TYPE("Class?")!><!DEBUG_INFO_EXPRESSION_TYPE("Class?")!><!DEBUG_INFO_EXPRESSION_TYPE("Class?")!><!DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!><!UNSAFE_CALL!>.<!>prop_8<!><!UNSAFE_CALL!>.<!>prop_8<!><!UNSAFE_CALL!>.<!>prop_8<!><!UNSAFE_CALL!>.<!>prop_8<!>
    }
}

// TESTCASE NUMBER: 5
fun <T> case_5(x: T) {
    if (x?.propNullableT != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
    }
}

// TESTCASE NUMBER: 6
fun <T>case_6(x: Inv<T>?) {
    if (x?.prop_1?.prop_1?.prop_1?.prop_2 != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.prop_1<!>.prop_1<!>.prop_1<!>.prop_2<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.prop_1<!>.prop_1<!>.prop_1<!>.prop_2<!>.equals(10)
    }
}

// TESTCASE NUMBER: 7
inline fun <reified T>case_7(x: Inv<T>?) {
    if (x?.prop_1?.prop_1?.prop_1?.prop_2 == null) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.prop_1<!>.prop_1<!>.prop_1<!>.prop_2<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!!"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.prop_1<!>.prop_1<!>.prop_1<!>.prop_2<!>.equals(10)
    }
}

// TESTCASE NUMBER: 8
fun <T>case_8(x: Inv<T>?) {
    if (x?.prop_1?.prop_1?.prop_1?.prop_1 == null) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<T> & Inv<T>?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.prop_1<!>.prop_1<!>.prop_1<!>.prop_1<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<T>"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.prop_1<!>.prop_1<!>.prop_1<!>.prop_1<!>.equals(10)
    }
}

// TESTCASE NUMBER: 9
inline fun <reified T>case_9(x: Out<T>?) {
    if (x?.prop_1?.prop_1?.prop_1?.prop_1 != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<T> & Inv<T>?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.prop_1<!>.prop_1<!>.prop_1<!>.prop_1<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<T>"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>x<!>.prop_1<!>.prop_1<!>.prop_1<!>.prop_1<!>.equals(10)
    }
}
