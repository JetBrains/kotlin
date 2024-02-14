// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 30
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, properties, objects, typealiases, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30376
 */
fun case_1(x: Class?) {
    if (x!!.prop_8?.prop_8?.prop_8?.prop_8 != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8.prop_8.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8.prop_8.prop_8.prop_8
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30376
 */
fun case_2(x: Class?) {
    if (x?.prop_8!!.prop_8?.prop_8?.prop_8 !== null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8.prop_8.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8.prop_8.prop_8.prop_8
    }
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30376
 */
fun case_3(x: Class?) {
    if (x?.prop_8?.prop_8?.prop_8!!.prop_8 == null) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8.prop_8.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8.prop_8.prop_8.prop_8.prop_8
    }
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30376
 */
fun case_4(x: Class?) {
    if (x!!<!UNNECESSARY_SAFE_CALL!>?.<!>prop_8?.prop_8?.prop_8?.prop_8 == null == true) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8.prop_8.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8.prop_8.prop_8.prop_8
    }
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30376
 */
fun case_5(x: Class?) {
    if (x?.prop_8!!<!UNNECESSARY_SAFE_CALL!>?.<!>prop_8?.prop_8?.prop_8 == null == true) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8.prop_8.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8.prop_8.prop_8.prop_8
    }
}

/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30376
 */
fun case_6(x: Class?) {
    if (x?.prop_8?.prop_8?.prop_8!!<!UNNECESSARY_SAFE_CALL!>?.<!>prop_8 == null == true) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8.prop_8.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8.prop_8.prop_8.prop_8.prop_8
    }
}

/*
 * TESTCASE NUMBER: 7
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30376
 */
fun case_7(x: Class) {
    if (x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.prop_8?.prop_8?.prop_8?.prop_8 != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>x<!>.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>x<!>.prop_8.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>x<!>.prop_8.prop_8.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>x<!>.prop_8.prop_8.prop_8.prop_8
    }
}

/*
 * TESTCASE NUMBER: 8
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30376
 */
fun case_8(x: Class) {
    if (x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.prop_8?.prop_8?.prop_8?.prop_8 != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>x<!>.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>x<!>.prop_8.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>x<!>.prop_8.prop_8.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>x<!>.prop_8.prop_8.prop_8.prop_8
    }
}
// TESTCASE NUMBER: 9
fun <T> case_9(x: T) {
    if (x!!.propNullableT != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        x.propNullableT
    }
}

/*
 * TESTCASE NUMBER: 10
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30376
 */
fun <T>case_10(x: Inv<T>?) {
    if (x!!.prop_1?.prop_1?.prop_1?.prop_2 != null) {
        x.prop_1.prop_1.prop_1.prop_2
        x.prop_1.prop_1.prop_1.prop_2.equals(10)
    }
}

/*
 * TESTCASE NUMBER: 11
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30376
 */
inline fun <reified T>case_11(x: Inv<T>?) {
    if (x?.prop_1!!.prop_1?.prop_1?.prop_2 == null) else {
        x.prop_1.prop_1.prop_1.prop_2
        x.prop_1.prop_1.prop_1.prop_2.equals(10)
    }
}

/*
 * TESTCASE NUMBER: 12
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30376
 */
fun <T>case_12(x: Inv<T>?) {
    if (x?.prop_1?.prop_1?.prop_1!!.prop_1 == null) else {
        x.prop_1.prop_1.prop_1.prop_1
        x.prop_1.prop_1.prop_1.prop_1.equals(10)
    }
}

/*
 * TESTCASE NUMBER: 13
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30376
 */
inline fun <reified T>case_13(x: Out<T>?) {
    if (x?.prop_1?.prop_1!!.prop_1?.prop_1 != null) {
        x.prop_1.prop_1.prop_1.prop_1
        x.prop_1.prop_1.prop_1.prop_1.equals(10)
    }
}
