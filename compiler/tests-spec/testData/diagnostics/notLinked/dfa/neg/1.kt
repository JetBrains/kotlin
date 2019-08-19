// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 1
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, functions, typealiases, properties, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1(x: Any?) {
    if (x != null is Boolean) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 3
fun case_3() {
    if (<!SENSELESS_COMPARISON!>Object.prop_1 == null !== null<!>)
    else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>Object.prop_1<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>Object.prop_1<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>Object.prop_1<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>Object.prop_1<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>Object.prop_1<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>Object.prop_1<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>Object.prop_1<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>Object.prop_1<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>Object.prop_1<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>Object.prop_1<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: Char?) {
    if (x != null || <!USELESS_IS_CHECK!>false is Boolean<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char?")!>x<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char?")!>x<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 5
fun case_5() {
    val x: Unit? = null

    if (<!EQUALITY_NOT_APPLICABLE!>x !== <!USELESS_IS_CHECK!>null is Boolean?<!><!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>
    if (<!SENSELESS_COMPARISON!>x !== null == null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!><!UNSAFE_CALL!>.<!>equals(null)
    if (<!SENSELESS_COMPARISON!>x !== null == null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>.propT
    if (<!SENSELESS_COMPARISON!>x !== null == null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!><!UNSAFE_CALL!>.<!>propAny
    if (<!SENSELESS_COMPARISON!>x !== null == null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>.propNullableT
    if (<!SENSELESS_COMPARISON!>x !== null == null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>.propNullableAny
    if (<!SENSELESS_COMPARISON!>x !== null == null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>.funT()
    if (<!SENSELESS_COMPARISON!>x !== null == null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
    if (<!SENSELESS_COMPARISON!>x !== null == null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>.funNullableT()
    if (<!SENSELESS_COMPARISON!>x !== null == null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>.funNullableAny()
}

// TESTCASE NUMBER: 6
fun case_6(x: EmptyClass?) {
    val y = true

    if (<!USELESS_IS_CHECK!>(x != null && !y) is Boolean<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass?")!>x<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass?")!>x<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 7
fun case_7() {
    if (nullableNumberProperty != null || <!EQUALITY_NOT_APPLICABLE!><!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.Number?")!>nullableNumberProperty<!> != null is Boolean<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>nullableNumberProperty<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>nullableNumberProperty<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>nullableNumberProperty<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>nullableNumberProperty<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>nullableNumberProperty<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>nullableNumberProperty<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>nullableNumberProperty<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>nullableNumberProperty<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>nullableNumberProperty<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>nullableNumberProperty<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 8
fun case_8(x: TypealiasNullableString) {
    if (<!SENSELESS_COMPARISON!>x !== null === null<!> && <!SENSELESS_COMPARISON!><!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String? */")!>x<!> != null != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String? */")!>x<!><!UNSAFE_CALL!>.<!>get(0)
    if (<!SENSELESS_COMPARISON!>x !== null != null<!> && <!SENSELESS_COMPARISON!><!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String? */")!>x<!> != null === null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String? */")!>x<!><!UNSAFE_CALL!>.<!>get(0)
}

// TESTCASE NUMBER: 9
fun case_9(x: TypealiasNullableString<!REDUNDANT_NULLABLE!>?<!>) {
    if (<!SENSELESS_COMPARISON!>x === null === null<!>) {

    } else if (<!USELESS_IS_CHECK!>false is Boolean<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? /* = kotlin.String? */")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? /* = kotlin.String? */")!>x<!><!UNSAFE_CALL!>.<!>get(0)
    }
}

// TESTCASE NUMBER: 10
fun case_10() {
    val a = Class()

    if (a.prop_4 === null || <!USELESS_IS_CHECK!>true is Boolean<!>) {
        if (<!SENSELESS_COMPARISON!>a.prop_4 != null !== null<!>) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float?")!>a.prop_4<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float?")!>a.prop_4<!><!UNSAFE_CALL!>.<!>equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float?")!>a.prop_4<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float?")!>a.prop_4<!><!UNSAFE_CALL!>.<!>propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float?")!>a.prop_4<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float?")!>a.prop_4<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float?")!>a.prop_4<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float?")!>a.prop_4<!><!UNSAFE_CALL!>.<!>funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float?")!>a.prop_4<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float?")!>a.prop_4<!>.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 11
fun case_11(x: TypealiasNullableStringIndirect<!REDUNDANT_NULLABLE!>?<!>, y: TypealiasNullableStringIndirect) {
    val t: TypealiasNullableStringIndirect = null

    if (<!EQUALITY_NOT_APPLICABLE!>x == null is Boolean<!>) {

    } else {
        if (<!EQUALITY_NOT_APPLICABLE!>y != null is Boolean<!> == true) {
            if (<!USELESS_IS_CHECK!>(nullableStringProperty == null) !is Boolean<!>) {
                if (<!EQUALITY_NOT_APPLICABLE!>t != null is Boolean<!>) {
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect? /* = kotlin.String? */")!>x<!>
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect? /* = kotlin.String? */")!>x<!>.equals(null)
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect? /* = kotlin.String? */")!>x<!>.propT
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect? /* = kotlin.String? */")!>x<!><!UNSAFE_CALL!>.<!>propAny
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect? /* = kotlin.String? */")!>x<!>.propNullableT
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect? /* = kotlin.String? */")!>x<!>.propNullableAny
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect? /* = kotlin.String? */")!>x<!>.funT()
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect? /* = kotlin.String? */")!>x<!><!UNSAFE_CALL!>.<!>funAny()
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect? /* = kotlin.String? */")!>x<!>.funNullableT()
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect? /* = kotlin.String? */")!>x<!>.funNullableAny()
                }
            }
        }
    }
}

// TESTCASE NUMBER: 12
fun case_12(x: TypealiasNullableStringIndirect, y: TypealiasNullableStringIndirect) =
    if (<!DEPRECATED_IDENTITY_EQUALS!><!USELESS_IS_CHECK!>(x == null) !is Boolean<!> === false<!>) "1"
    else if (<!USELESS_IS_CHECK!>(<!SENSELESS_COMPARISON!>y === null !== null<!>) is Boolean<!>) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect /* = kotlin.String? */")!>x<!>
    else if (<!SENSELESS_COMPARISON!>y === null != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect /* = kotlin.String? */")!>x<!>.equals(null)
    else if (<!SENSELESS_COMPARISON!>y === null != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect /* = kotlin.String? */")!>x<!>.propT
    else if (<!SENSELESS_COMPARISON!>y === null != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect /* = kotlin.String? */")!>x<!><!UNSAFE_CALL!>.<!>propAny
    else if (<!SENSELESS_COMPARISON!>y === null != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect /* = kotlin.String? */")!>x<!>.propNullableT
    else if (<!SENSELESS_COMPARISON!>y === null != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect /* = kotlin.String? */")!>x<!>.propNullableAny
    else if (<!SENSELESS_COMPARISON!>y === null != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect /* = kotlin.String? */")!>x<!>.funT()
    else if (<!SENSELESS_COMPARISON!>y === null != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect /* = kotlin.String? */")!>x<!><!UNSAFE_CALL!>.<!>funAny()
    else if (<!SENSELESS_COMPARISON!>y === null != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect /* = kotlin.String? */")!>x<!>.funNullableT()
    else if (<!SENSELESS_COMPARISON!>y === null != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect /* = kotlin.String? */")!>x<!>.funNullableAny()
    else "-1"

// TESTCASE NUMBER: 13
fun <!IMPLICIT_NOTHING_RETURN_TYPE!>case_13<!>(x: <!UNRESOLVED_REFERENCE!>otherpackage<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>Case13<!>?) =
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>if (<!DEPRECATED_IDENTITY_EQUALS!>(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>==<!> null !is Boolean) !== true<!>) {
        throw Exception()
    } else {
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_EXPRESSION_TYPE("[ERROR : otherpackage.Case13]?")!>x<!>
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_EXPRESSION_TYPE("[ERROR : otherpackage.Case13]?")!>x<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>equals<!>(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!>)
    }<!>

// TESTCASE NUMBER: 14
class Case14 {
    val x: <!UNRESOLVED_REFERENCE!>otherpackage<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>Case14<!>?
    init {
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> = <!UNRESOLVED_REFERENCE!>otherpackage<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>Case14<!>()
    }
}

fun case_14() {
    val a = Case14()

    if (a.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>!=<!> <!USELESS_IS_CHECK!>null !is Boolean !is Boolean<!>) {
        if (a.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>!=<!> null == true) {
            if (a.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> !== null == false) {
                if (<!SENSELESS_COMPARISON!>a.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>!=<!> null == null<!>) {
                    if (<!SENSELESS_COMPARISON!>a.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>!=<!> null !== null<!>) {
                        if (<!DEPRECATED_IDENTITY_EQUALS!>a.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>!=<!> null === true<!>) {
                            if (<!DEPRECATED_IDENTITY_EQUALS!>a.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> !== null === <!USELESS_IS_CHECK!>true !is Boolean<!><!> == true) {
                                if (<!DEPRECATED_IDENTITY_EQUALS!>a.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>!=<!> null !== false<!>) {
                                    if (<!DEPRECATED_IDENTITY_EQUALS!>a.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>!=<!> null === false<!>) {
                                        if (<!DEPRECATED_IDENTITY_EQUALS!>a.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> !== null === true<!>) {
                                            if (<!USELESS_IS_CHECK!>(a.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>!=<!> null != true) !is Boolean<!>) {
                                                if (a.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>!=<!> null is Boolean) {
                                                    if (a.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>!=<!> <!USELESS_IS_CHECK!>null is Boolean is Boolean<!>) {
                                                        if (<!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>a.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> !== null is Boolean<!>) {
                                                            if (a.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>!=<!> null is Boolean) {
                                                                if ((<!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>a.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> !== null !is Boolean<!>) == false) {
                                                                    <!DEBUG_INFO_EXPRESSION_TYPE("[ERROR : otherpackage.Case14]?")!>a.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!><!>
                                                                    <!DEBUG_INFO_EXPRESSION_TYPE("[ERROR : otherpackage.Case14]?")!>a.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!><!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>equals<!>(a.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!>)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// TESTCASE NUMBER: 15
fun case_15(x: EmptyObject) {
    val <!UNUSED_VARIABLE!>t<!> = if (<!EQUALITY_NOT_APPLICABLE!>x === <!USELESS_IS_CHECK!><!USELESS_IS_CHECK!>null is Boolean is Boolean<!> is Boolean<!><!>) "" else {
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 16
fun case_16() {
    val x: TypealiasNullableNothing = null

    if (<!DEBUG_INFO_CONSTANT!>x<!> != <!USELESS_IS_CHECK!><!USELESS_IS_CHECK!><!USELESS_IS_CHECK!><!USELESS_IS_CHECK!>null !is Boolean !is Boolean<!> !is Boolean<!> !is Boolean<!> !is Boolean<!>) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableNothing /* = kotlin.Nothing? */")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableNothing /* = kotlin.Nothing? */")!>x<!><!UNSAFE_CALL!>.<!>java
    }
}

// TESTCASE NUMBER: 17
val case_17 = if (nullableIntProperty == null == true == false) 0 else {
    <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Nothing?")!>nullableIntProperty<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Nothing?")!>nullableIntProperty<!><!UNSAFE_CALL!>.<!>java
}

//TESTCASE NUMBER: 18
fun case_18(a: DeepObject.A.B.C.D.E.F.G.J?) {
    if (<!SENSELESS_COMPARISON!>a != null !== null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 19
fun case_19(b: Boolean) {
    val a = if (b) {
        object {
            val B19 = if (b) {
                object {
                    val C19 = if (b) {
                        object {
                            val D19 = if (b) {
                                object {
                                    val x: Number? = 10
                                }
                            } else null
                        }
                    } else null
                }
            } else null
        }
    } else null

    if (<!EQUALITY_NOT_APPLICABLE!>a != null !is Boolean<!> && <!EQUALITY_NOT_APPLICABLE!>a<!UNSAFE_CALL!>.<!>B19 != null is Boolean<!> && <!EQUALITY_NOT_APPLICABLE!>a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19 != null is Boolean<!> && <!SENSELESS_COMPARISON!>a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19 != null == null<!> && <!SENSELESS_COMPARISON!>a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19<!UNSAFE_CALL!>.<!>x != null !== null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19<!UNSAFE_CALL!>.<!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19<!UNSAFE_CALL!>.<!>x<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19<!UNSAFE_CALL!>.<!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19<!UNSAFE_CALL!>.<!>x<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19<!UNSAFE_CALL!>.<!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19<!UNSAFE_CALL!>.<!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19<!UNSAFE_CALL!>.<!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19<!UNSAFE_CALL!>.<!>x<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19<!UNSAFE_CALL!>.<!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19<!UNSAFE_CALL!>.<!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 20
fun case_20(b: Boolean) {
    val a = object {
        val B19 = object {
            val C19 = object {
                val D19 =  if (b) {
                    object {}
                } else null
            }
        }
    }

    if (<!EQUALITY_NOT_APPLICABLE!>a.B19.C19.D19 !== null !is Boolean<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>?")!>a.B19.C19.D19<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>?")!>a.B19.C19.D19<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>?")!>a.B19.C19.D19<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>?")!>a.B19.C19.D19<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>?")!>a.B19.C19.D19<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>?")!>a.B19.C19.D19<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>?")!>a.B19.C19.D19<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>?")!>a.B19.C19.D19<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>?")!>a.B19.C19.D19<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>?")!>a.B19.C19.D19<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 21
fun case_21() {
    if (<!EQUALITY_NOT_APPLICABLE!>EnumClassWithNullableProperty.B.prop_1 !== null is Boolean<!> == <!USELESS_IS_CHECK!>true !is Boolean<!> != true) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>EnumClassWithNullableProperty.B.prop_1<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>EnumClassWithNullableProperty.B.prop_1<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>EnumClassWithNullableProperty.B.prop_1<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>EnumClassWithNullableProperty.B.prop_1<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>EnumClassWithNullableProperty.B.prop_1<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>EnumClassWithNullableProperty.B.prop_1<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>EnumClassWithNullableProperty.B.prop_1<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>EnumClassWithNullableProperty.B.prop_1<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>EnumClassWithNullableProperty.B.prop_1<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>EnumClassWithNullableProperty.B.prop_1<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 22
fun case_22(a: (() -> Unit)?) {
    if (<!EQUALITY_NOT_APPLICABLE!>a != null !is Boolean<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!UNSAFE_CALL!>a<!>()<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!UNSAFE_CALL!>a<!>()<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!UNSAFE_CALL!>a<!>()<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!UNSAFE_CALL!>a<!>()<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!UNSAFE_CALL!>a<!>()<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!UNSAFE_CALL!>a<!>()<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!UNSAFE_CALL!>a<!>()<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!UNSAFE_CALL!>a<!>()<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!UNSAFE_CALL!>a<!>()<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!UNSAFE_CALL!>a<!>()<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 23
fun case_23(a: ((Float) -> Int?)?, b: Float?) {
    if (<!EQUALITY_NOT_APPLICABLE!>a != null !is Boolean<!> && <!EQUALITY_NOT_APPLICABLE!>b !== null is Boolean<!>) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!><!UNSAFE_CALL!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float?"), TYPE_MISMATCH!>b<!>)<!>
        if (x != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 24
fun case_24(a: ((() -> Unit) -> Unit)?, b: (() -> Unit)?) =
    if (<!EQUALITY_NOT_APPLICABLE!>a !== null is Boolean<!> && <!EQUALITY_NOT_APPLICABLE!>b !== null !is Boolean<!>) {
        <!UNSAFE_CALL!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)?"), TYPE_MISMATCH!>b<!>)
        <!UNSAFE_CALL!>a<!>(<!TYPE_MISMATCH!>b<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)?")!>b<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)?")!>b<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)?")!>b<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)?")!>b<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)?")!>b<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)?")!>b<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)?")!>b<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)?")!>b<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)?")!>b<!>.funNullableAny()
    } else null

// TESTCASE NUMBER: 25
fun case_25(b: Boolean) {
    val x = {
        if (b) object {
            val a = 10
        } else null
    }

    val y = if (b) x else null

    if (<!DEPRECATED_IDENTITY_EQUALS!>y !== null === true<!>) {
        val z = <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided>?")!><!UNSAFE_CALL!>y<!>()<!>

        if (<!DEPRECATED_IDENTITY_EQUALS!>z != null !== false<!>) {
            <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided>?")!>z<!><!UNSAFE_CALL!>.<!>a
            <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided>?")!>z<!><!UNSAFE_CALL!>.<!>a.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided>?")!>z<!><!UNSAFE_CALL!>.<!>a.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided>?")!>z<!><!UNSAFE_CALL!>.<!>a.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided>?")!>z<!><!UNSAFE_CALL!>.<!>a.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided>?")!>z<!><!UNSAFE_CALL!>.<!>a.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided>?")!>z<!><!UNSAFE_CALL!>.<!>a.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided>?")!>z<!><!UNSAFE_CALL!>.<!>a.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided>?")!>z<!><!UNSAFE_CALL!>.<!>a.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided>?")!>z<!><!UNSAFE_CALL!>.<!>a.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 26
fun case_26(a: ((Float) -> Int?)?, b: Float?) {
    if (a != null == true == false && b != null == true == false) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!><!UNSAFE_IMPLICIT_INVOKE_CALL!>a<!>(<!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float? & kotlin.Nothing?"), TYPE_MISMATCH!>b<!>)<!>
        if (<!DEPRECATED_IDENTITY_EQUALS!>x != null == true === false<!>) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!><!UNSAFE_CALL!>.<!>equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!><!UNSAFE_CALL!>.<!>propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!>.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 27
fun case_27() {
    if (Object.prop_1 == null == true == true == true == true == true == true == true == true == true == true == true == true == true == <!USELESS_IS_CHECK!>true is Boolean<!>)
    else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>Object.prop_1<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>Object.prop_1<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>Object.prop_1<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>Object.prop_1<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>Object.prop_1<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>Object.prop_1<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>Object.prop_1<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>Object.prop_1<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>Object.prop_1<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>Object.prop_1<!>.funNullableAny()
    }
}