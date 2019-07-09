// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 28
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1(x: Int?) {
    if (x?.inv() != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.inv()
    }
}

// TESTCASE NUMBER: 2
fun case_2(x: Int?) {
    if (x?.inv() == null) else if (true) {} else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.inv()
    }
}

// TESTCASE NUMBER: 3
fun case_3(x: Boolean?) {
    if (x?.not() == null) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: EnumClass?) {
    if (x?.fun_1() !== null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("EnumClass & EnumClass?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("EnumClass & EnumClass?"), DEBUG_INFO_SMARTCAST!>x<!>.fun_1()
    }
}

// TESTCASE NUMBER: 5
fun case_5(x: Class.NestedClass?) {
    if (x?.prop_4 != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class.NestedClass & Class.NestedClass?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class.NestedClass & Class.NestedClass?"), DEBUG_INFO_SMARTCAST!>x<!>.prop_4
    }
}

// TESTCASE NUMBER: 6
fun case_6(x: Class.NestedClass?) {
    if (!(x?.prop_4 == null)) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class.NestedClass & Class.NestedClass?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class.NestedClass & Class.NestedClass?"), DEBUG_INFO_SMARTCAST!>x<!>.prop_4
    }
}

// TESTCASE NUMBER: 7
fun case_7(x: DeepObject.A.B.C.D.E.F.G.J?) {
    if (!!(x?.prop_1 != null)) {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>x<!>.prop_1
    }
}

// TESTCASE NUMBER: 8
fun case_8(x: Any?) {
    if (x?.equals(10) === null) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(10)
    }
}

// TESTCASE NUMBER: 9
fun case_9(x: Any?) {
    if (x?.equals(10) !== null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(10)
    }
}

/*
 * TESTCASE NUMBER: 10
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30369
 */
fun case_10(x: Interface3?) {
    if (x?.itest() == null == true) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3?")!>x<!><!UNSAFE_CALL!>.<!>itest()
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3?")!>x<!><!UNSAFE_CALL!>.<!>itest3()
    }
}

/*
 * TESTCASE NUMBER: 11
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30369
 */
fun case_11(x: SealedMixedChildObject1?) {
    if (x?.prop_1 != null == true) {
        <!DEBUG_INFO_EXPRESSION_TYPE("SealedMixedChildObject1?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("SealedMixedChildObject1?")!>x<!><!UNSAFE_CALL!>.<!>prop_1
        <!DEBUG_INFO_EXPRESSION_TYPE("SealedMixedChildObject1?")!>x<!><!UNSAFE_CALL!>.<!>prop_2
    }
}

// TESTCASE NUMBER: 12
inline fun <reified T>case_12(x: Any?) {
    if (x?.equals(10) != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(10)
    }
}

// TESTCASE NUMBER: 13
inline fun <reified T>case_13(x: Any?) {
    if (x?.equals(10) == null) {} else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(10)
    }
}

// TESTCASE NUMBER: 14
inline fun <reified T>case_14(x: Any?) {
    if (x?.equals(10) === null) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(10)
    }
}

// TESTCASE NUMBER: 15
inline fun <reified T>case_15(x: Any?) {
    if (x?.equals(10) !== null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(10)
    }
}

/*
 * TESTCASE NUMBER: 16
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30369, KT-28262
 */
inline fun <reified T>case_16(x: Any?) {
    if (x?.equals(10) === null == true) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
    }
}

/*
 * TESTCASE NUMBER: 17
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30369, KT-28262
 */
inline fun <reified T>case_17(x: Any?) {
    if (x?.equals(10) !== null == true) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
    }
}

/*
 * TESTCASE NUMBER: 18
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30369, KT-28262, KT-29878
 */
inline fun <reified T>case_18(x: Any?) {
    if (<!DEPRECATED_IDENTITY_EQUALS!>x?.equals(10) === null === true<!>) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
    }
}

/*
 * TESTCASE NUMBER: 19
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30369, KT-28262, KT-29878
 */
inline fun <reified T>case_19(x: Any?) {
    if (<!DEPRECATED_IDENTITY_EQUALS!>x?.equals(10) !== null === true<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
    }
}

/*
 * TESTCASE NUMBER: 20
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30369, KT-28262, KT-29878
 */
inline fun <reified T>case_20(x: Any?) {
    if (<!DEPRECATED_IDENTITY_EQUALS!>x?.equals(10) === null !== false<!>) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
    }
}

/*
 * TESTCASE NUMBER: 21
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30369, KT-28262, KT-29878
 */
inline fun <reified T>case_21(x: Any?) {
    if (<!DEPRECATED_IDENTITY_EQUALS!>x?.equals(10) !== null !== false<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
    }
}

/*
 * TESTCASE NUMBER: 22
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30369, KT-28262, KT-29878
 */
inline fun <reified T>case_22(x: Any?) {
    if (<!DEPRECATED_IDENTITY_EQUALS!>x?.equals(10) !== null !== true<!>) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
    }
}

/*
 * TESTCASE NUMBER: 23
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30369, KT-28262, KT-29878
 */
inline fun <reified T>case_23(x: Any?) {
    if (<!DEPRECATED_IDENTITY_EQUALS!>x?.equals(10) === null === false<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
    }
}

/*
 * TESTCASE NUMBER: 24
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30369, KT-28262
 */
inline fun <reified T>case_24(x: Any?) {
    if (x?.equals(10) !== null != true) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
    }
}

/*
 * TESTCASE NUMBER: 25
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30369, KT-28262
 */
inline fun <reified T>case_25(x: Any?) {
    if (x?.equals(10) === null == false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
    }
}

/*
 * TESTCASE NUMBER: 26
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30369, KT-29878
 */
inline fun <reified T>case_26(x: Any?) {
    if (<!DEPRECATED_IDENTITY_EQUALS!>x?.equals(10) != null === false<!>) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
    }
}

/*
 * TESTCASE NUMBER: 27
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30369, KT-29878
 */
inline fun <reified T>case_27(x: Any?) {
    if (<!DEPRECATED_IDENTITY_EQUALS!>x?.equals(10) == null === false<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
    }
}

/*
 * TESTCASE NUMBER: 28
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30369
 */
inline fun <reified T>case_28(x: Any?) {
    if (x?.equals(10) != null == false) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
    }
}

/*
 * TESTCASE NUMBER: 29
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30369
 */
inline fun <reified T>case_29(x: Any?) {
    if (x?.equals(10) == null == false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
    }
}

/*
 * TESTCASE NUMBER: 30
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30369
 */
fun case_30(x: Class.NestedClass?) {
    if (x?.prop_4 != null == true) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class.NestedClass?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class.NestedClass?")!>x<!><!UNSAFE_CALL!>.<!>prop_4
    }
}

/*
 * TESTCASE NUMBER: 31
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30369
 */
fun case_31(x: Class.NestedClass?) {
    if (!(x?.prop_4 == null) != false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class.NestedClass?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class.NestedClass?")!>x<!><!UNSAFE_CALL!>.<!>prop_4
    }
}

/*
 * TESTCASE NUMBER: 32
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30369
 */
fun case_32(x: Class.NestedClass?) {
    if (x?.prop_4 == null == false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class.NestedClass?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class.NestedClass?")!>x<!><!UNSAFE_CALL!>.<!>prop_4
    }
}

/*
 * TESTCASE NUMBER: 33
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30369
 */
fun case_33(x: Class.NestedClass?) {
    if (!(x?.prop_4 != null) != true) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class.NestedClass?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class.NestedClass?")!>x<!><!UNSAFE_CALL!>.<!>prop_4
    }
}
