// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

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
    if (Object.prop_1 == null !== null)
    else {
        Object.prop_1
        Object.prop_1<!UNSAFE_CALL!>.<!>equals(null)
        Object.prop_1.propT
        Object.prop_1<!UNSAFE_CALL!>.<!>propAny
        Object.prop_1.propNullableT
        Object.prop_1.propNullableAny
        Object.prop_1.funT()
        Object.prop_1<!UNSAFE_CALL!>.<!>funAny()
        Object.prop_1.funNullableT()
        Object.prop_1.funNullableAny()
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: Char?) {
    if (x != null || false is Boolean) {
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

    if (x !== null is Boolean?) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>
    if (x !== null == null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!><!UNSAFE_CALL!>.<!>equals(null)
    if (x !== null == null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>.propT
    if (x !== null == null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!><!UNSAFE_CALL!>.<!>propAny
    if (x !== null == null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>.propNullableT
    if (x !== null == null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>.propNullableAny
    if (x !== null == null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>.funT()
    if (x !== null == null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
    if (x !== null == null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>.funNullableT()
    if (x !== null == null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>.funNullableAny()
}

// TESTCASE NUMBER: 6
fun case_6(x: EmptyClass?) {
    val y = true

    if ((x != null && !y) is Boolean) {
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
    if (nullableNumberProperty != null || <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>nullableNumberProperty<!> != null is Boolean) {
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
    if (x !== null === null && <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!> != null != null) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.<!INAPPLICABLE_CANDIDATE!>get<!>(0)
    if (x !== null != null && <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!> != null === null) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.<!INAPPLICABLE_CANDIDATE!>get<!>(0)
}

// TESTCASE NUMBER: 9
fun case_9(x: TypealiasNullableString?) {
    if (x === null === null) {

    } else if (false is Boolean) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!><!UNSAFE_CALL!>.<!>get(0)
    }
}

// TESTCASE NUMBER: 10
fun case_10() {
    val a = Class()

    if (a.prop_4 === null || true is Boolean) {
        if (a.prop_4 != null !== null) {
            a.prop_4
            a.prop_4<!UNSAFE_CALL!>.<!>equals(null)
            a.prop_4.propT
            a.prop_4<!UNSAFE_CALL!>.<!>propAny
            a.prop_4.propNullableT
            a.prop_4.propNullableAny
            a.prop_4.funT()
            a.prop_4<!UNSAFE_CALL!>.<!>funAny()
            a.prop_4.funNullableT()
            a.prop_4.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 11
fun case_11(x: TypealiasNullableStringIndirect?, y: TypealiasNullableStringIndirect) {
    val t: TypealiasNullableStringIndirect = null

    if (x == null is Boolean) {

    } else {
        if (y != null is Boolean == true) {
            if ((nullableStringProperty == null) !is Boolean) {
                if (t != null is Boolean) {
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect?")!>x<!>
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect?")!>x<!>.equals(null)
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect?")!>x<!>.propT
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect?")!>x<!><!UNSAFE_CALL!>.<!>propAny
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect?")!>x<!>.propNullableT
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect?")!>x<!>.propNullableAny
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect?")!>x<!>.funT()
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect?")!>x<!>.funNullableT()
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect?")!>x<!>.funNullableAny()
                }
            }
        }
    }
}

// TESTCASE NUMBER: 12
fun case_12(x: TypealiasNullableStringIndirect, y: TypealiasNullableStringIndirect) =
    if ((x == null) !is Boolean === false) "1"
    else if ((y === null !== null) is Boolean) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect")!>x<!>
    else if (y === null != null) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect")!>x<!>.equals(null)
    else if (y === null != null) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect")!>x<!>.propT
    else if (y === null != null) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect")!>x<!>.<!INAPPLICABLE_CANDIDATE!>propAny<!>
    else if (y === null != null) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect")!>x<!>.propNullableT
    else if (y === null != null) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect")!>x<!>.propNullableAny
    else if (y === null != null) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect")!>x<!>.funT()
    else if (y === null != null) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect")!>x<!>.<!INAPPLICABLE_CANDIDATE!>funAny<!>()
    else if (y === null != null) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect")!>x<!>.funNullableT()
    else if (y === null != null) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect")!>x<!>.funNullableAny()
    else "-1"

// TESTCASE NUMBER: 13
fun case_13(x: <!UNRESOLVED_REFERENCE!>otherpackage.Case13?<!>) =
    <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Can't resolve when expression")!>if ((x == null !is Boolean) !== true) {
        throw Exception()
    } else {
        <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Symbol not found, for `otherpackage.Case13?`")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Symbol not found, for `otherpackage.Case13?`")!>x<!>.<!AMBIGUITY!>equals<!>(x)
    }<!>

// TESTCASE NUMBER: 14
class Case14 {
    val x: <!UNRESOLVED_REFERENCE!>otherpackage.Case14?<!>
    init {
        x = <!UNRESOLVED_REFERENCE!>otherpackage<!>.<!UNRESOLVED_REFERENCE!>Case14<!>()
    }
}

fun case_14() {
    val a = Case14()

    if (a.x != null !is Boolean !is Boolean) {
        if (a.x != null == true) {
            if (a.x !== null == false) {
                if (a.x != null == null) {
                    if (a.x != null !== null) {
                        if (a.x != null === true) {
                            if (a.x !== null === true !is Boolean == true) {
                                if (a.x != null !== false) {
                                    if (a.x != null === false) {
                                        if (a.x !== null === true) {
                                            if ((a.x != null != true) !is Boolean) {
                                                if (a.x != null is Boolean) {
                                                    if (a.x != null is Boolean is Boolean) {
                                                        if (a.x !== null is Boolean) {
                                                            if (a.x != null is Boolean) {
                                                                if ((a.x !== null !is Boolean) == false) {
                                                                    a.x
                                                                    a.x.equals(a.x)
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
    val t = if (x === null is Boolean is Boolean is Boolean) "" else {
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

    if (x != null !is Boolean !is Boolean !is Boolean !is Boolean !is Boolean) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableNothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableNothing")!>x<!>.<!INAPPLICABLE_CANDIDATE!>java<!>
    }
}

// TESTCASE NUMBER: 17
val case_17 = if (nullableIntProperty == null == true == false) 0 else {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>nullableIntProperty<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>nullableIntProperty<!>.<!UNRESOLVED_REFERENCE!>java<!>
}

//TESTCASE NUMBER: 18
fun case_18(a: DeepObject.A.B.C.D.E.F.G.J?) {
    if (a != null !== null) {
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

    if (a != null !is Boolean && a<!UNSAFE_CALL!>.<!>B19 != null is Boolean && a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19 != null is Boolean && a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19 != null == null && a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19<!UNSAFE_CALL!>.<!>x != null !== null) {
        a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19<!UNSAFE_CALL!>.<!>x
        a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19<!UNSAFE_CALL!>.<!>x<!UNSAFE_CALL!>.<!>equals(null)
        a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19<!UNSAFE_CALL!>.<!>x.propT
        a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19<!UNSAFE_CALL!>.<!>x<!UNSAFE_CALL!>.<!>propAny
        a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19<!UNSAFE_CALL!>.<!>x.propNullableT
        a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19<!UNSAFE_CALL!>.<!>x.propNullableAny
        a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19<!UNSAFE_CALL!>.<!>x.funT()
        a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19<!UNSAFE_CALL!>.<!>x<!UNSAFE_CALL!>.<!>funAny()
        a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19<!UNSAFE_CALL!>.<!>x.funNullableT()
        a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19<!UNSAFE_CALL!>.<!>x.funNullableAny()
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

    if (a.B19.C19.D19 !== null !is Boolean) {
        a.B19.C19.D19
        a.B19.C19.D19<!UNSAFE_CALL!>.<!>equals(null)
        a.B19.C19.D19.propT
        a.B19.C19.D19<!UNSAFE_CALL!>.<!>propAny
        a.B19.C19.D19.propNullableT
        a.B19.C19.D19.propNullableAny
        a.B19.C19.D19.funT()
        a.B19.C19.D19<!UNSAFE_CALL!>.<!>funAny()
        a.B19.C19.D19.funNullableT()
        a.B19.C19.D19.funNullableAny()
    }
}

// TESTCASE NUMBER: 21
fun case_21() {
    if (EnumClassWithNullableProperty.B.prop_1 !== null is Boolean == true !is Boolean != true) {
        EnumClassWithNullableProperty.B.prop_1
        EnumClassWithNullableProperty.B.prop_1<!UNSAFE_CALL!>.<!>equals(null)
        EnumClassWithNullableProperty.B.prop_1.propT
        EnumClassWithNullableProperty.B.prop_1<!UNSAFE_CALL!>.<!>propAny
        EnumClassWithNullableProperty.B.prop_1.propNullableT
        EnumClassWithNullableProperty.B.prop_1.propNullableAny
        EnumClassWithNullableProperty.B.prop_1.funT()
        EnumClassWithNullableProperty.B.prop_1<!UNSAFE_CALL!>.<!>funAny()
        EnumClassWithNullableProperty.B.prop_1.funNullableT()
        EnumClassWithNullableProperty.B.prop_1.funNullableAny()
    }
}

// TESTCASE NUMBER: 22
fun case_22(a: (() -> Unit)?) {
    if (a != null !is Boolean) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!UNSAFE_IMPLICIT_INVOKE_CALL!>a<!>()<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!UNSAFE_IMPLICIT_INVOKE_CALL!>a<!>()<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!UNSAFE_IMPLICIT_INVOKE_CALL!>a<!>()<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!UNSAFE_IMPLICIT_INVOKE_CALL!>a<!>()<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!UNSAFE_IMPLICIT_INVOKE_CALL!>a<!>()<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!UNSAFE_IMPLICIT_INVOKE_CALL!>a<!>()<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!UNSAFE_IMPLICIT_INVOKE_CALL!>a<!>()<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!UNSAFE_IMPLICIT_INVOKE_CALL!>a<!>()<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!UNSAFE_IMPLICIT_INVOKE_CALL!>a<!>()<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!UNSAFE_IMPLICIT_INVOKE_CALL!>a<!>()<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 23
fun case_23(a: ((Float) -> Int?)?, b: Float?) {
    if (a != null !is Boolean && b !== null is Boolean) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!><!UNSAFE_IMPLICIT_INVOKE_CALL!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float?")!>b<!>)<!>
        if (x != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>x<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>x<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>x<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>x<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>x<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>x<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>x<!>.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 24
fun case_24(a: ((() -> Unit) -> Unit)?, b: (() -> Unit)?) =
    if (a !== null is Boolean && b !== null !is Boolean) {
        <!UNSAFE_IMPLICIT_INVOKE_CALL!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!>)
        <!UNSAFE_IMPLICIT_INVOKE_CALL!>a<!>(b)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!>.funNullableAny()
    } else null

// TESTCASE NUMBER: 25
fun case_25(b: Boolean) {
    val x = {
        if (b) object {
            val a = 10
        } else null
    }

    val y = if (b) x else null

    if (y !== null === true) {
        val z = <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>y()<!>

        if (z != null !== false) {
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>? & <anonymous>")!>z<!>.a
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>? & <anonymous>")!>z<!>.a.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>? & <anonymous>")!>z<!>.a.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>? & <anonymous>")!>z<!>.a.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>? & <anonymous>")!>z<!>.a.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>? & <anonymous>")!>z<!>.a.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>? & <anonymous>")!>z<!>.a.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>? & <anonymous>")!>z<!>.a.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>? & <anonymous>")!>z<!>.a.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>? & <anonymous>")!>z<!>.a.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 26
fun case_26(a: ((Float) -> Int?)?, b: Float?) {
    if (a != null == true == false && b != null == true == false) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!><!UNSAFE_IMPLICIT_INVOKE_CALL!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float?")!>b<!>)<!>
        if (x != null == true === false) {
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
    if (Object.prop_1 == null == true == true == true == true == true == true == true == true == true == true == true == true == true == true is Boolean)
    else {
        Object.prop_1
        Object.prop_1<!UNSAFE_CALL!>.<!>equals(null)
        Object.prop_1.propT
        Object.prop_1<!UNSAFE_CALL!>.<!>propAny
        Object.prop_1.propNullableT
        Object.prop_1.propNullableAny
        Object.prop_1.funT()
        Object.prop_1<!UNSAFE_CALL!>.<!>funAny()
        Object.prop_1.funNullableT()
        Object.prop_1.funNullableAny()
    }
}
