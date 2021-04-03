// FIR_IGNORE
// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

// FILE: other_types.kt

package othertypes

// TESTCASE NUMBER: 12, 48
class EmptyClass12_48 {}

// FILE: main.kt

import othertypes.*

// TESTCASE NUMBER: 1
fun case_1(x: Any?) {
    val y = null
    if (x != y) {
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

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28159
 */
fun case_2(x: Nothing?) {
    val y = null
    if (x !== y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>.hashCode()
    }
}

// TESTCASE NUMBER: 3
fun case_3() {
    val y = null

    if (Object.prop_1 == y)
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
fun case_4(x: Char?, y: Nothing?) {
    if (x != y && true) {
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
    val y: Nothing? = null

    if (x !== y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 6
fun case_6(x: EmptyClass?, z: Nothing?) {
    val y = true

    if (x != z && !y) {
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
fun case_7(x: EmptyObject?) {
    val y = null

    if (x != y || <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject?")!>x<!> != y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject?")!>x<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject?")!>x<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 8
fun case_8(x: TypealiasNullableString) {
    val y = null

    if (x !== y && <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!> != y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 9
fun case_9(x: TypealiasNullableString?, y: Nothing?) {
    if (x === y) {

    } else if (false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 10
fun case_10() {
    val a = Class()
    val b = null

    if (a.prop_4 === b || true) {
        if (a.prop_4 != null) {
            a.prop_4
            a.prop_4.equals(null)
            a.prop_4.propT
            a.prop_4.propAny
            a.prop_4.propNullableT
            a.prop_4.propNullableAny
            a.prop_4.funT()
            a.prop_4.funAny()
            a.prop_4.funNullableT()
            a.prop_4.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 11
fun case_11(x: TypealiasNullableString?, y: TypealiasNullableString) {
    val z = null
    val u: TypealiasNullableString = null
    val v = null

    if (x == z && x == v) {

    } else {
        if (y != z) {
            if (nullableStringProperty == z) {
                if (u != z || u != v) {
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>.equals(null)
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>.propT
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!><!UNSAFE_CALL!>.<!>propAny
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>.propNullableT
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>.propNullableAny
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>.funT()
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>.funNullableT()
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>.funNullableAny()
                }
            }
        }
    }
}

// TESTCASE NUMBER: 12
fun case_12(x: TypealiasNullableString, y: TypealiasNullableString, z1: Nothing?, z2: Nothing?) = if (x == z1 || x == z2) "1"
    else if (y === z1 && y == z2) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.funNullableAny()
    } else "-1"

// TESTCASE NUMBER: 13
fun case_13(x: EmptyClass12_48?, z: Nothing?) =
    if (x == z || x === z && x == z) {
        throw Exception()
    } else {
        <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.EmptyClass12_48?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.EmptyClass12_48?")!>x<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.EmptyClass12_48?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.EmptyClass12_48?")!>x<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.EmptyClass12_48?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.EmptyClass12_48?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.EmptyClass12_48?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.EmptyClass12_48?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.EmptyClass12_48?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.EmptyClass12_48?")!>x<!>.funNullableAny()
    }

// TESTCASE NUMBER: 14
class Case14 {
    val x: TypealiasNullableString?
    init {
        x = TypealiasNullableString()
    }
}

fun case_14() {
    val a = Case14()
    val x = null
    val y = implicitNullableNothingProperty

    if (a.x != x && a.x != y || a.x != y && a.x !== null) {
        a.x
        a.x.equals(null)
        a.x.propT
        a.x<!UNSAFE_CALL!>.<!>propAny
        a.x.propNullableT
        a.x.propNullableAny
        a.x.funT()
        a.x<!UNSAFE_CALL!>.<!>funAny()
        a.x.funNullableT()
        a.x.funNullableAny()
    }
}

// TESTCASE NUMBER: 15
fun case_15(x: TypealiasNullableString) {
    val y = null
    val z = if (x === null || y == x && x === y || null === x) "" else {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString & kotlin.Any & TypealiasNullableString")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString & kotlin.Any & TypealiasNullableString")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString & kotlin.Any & TypealiasNullableString")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString & kotlin.Any & TypealiasNullableString")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString & kotlin.Any & TypealiasNullableString")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString & kotlin.Any & TypealiasNullableString")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString & kotlin.Any & TypealiasNullableString")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString & kotlin.Any & TypealiasNullableString")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString & kotlin.Any & TypealiasNullableString")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString & kotlin.Any & TypealiasNullableString")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 16
fun case_16() {
    val x: TypealiasNullableNothing = null
    val y: Nothing? = null

    if (x !== y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableNothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableNothing")!>x<!>.hashCode()
    }
}

// TESTCASE NUMBER: 17
val case_17 = if (nullableIntProperty === implicitNullableNothingProperty) 0 else {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>nullableIntProperty<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>nullableIntProperty<!><!UNSAFE_CALL!>.<!>equals(null)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>nullableIntProperty<!>.propT
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>nullableIntProperty<!><!UNSAFE_CALL!>.<!>propAny
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>nullableIntProperty<!>.propNullableT
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>nullableIntProperty<!>.propNullableAny
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>nullableIntProperty<!>.funT()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>nullableIntProperty<!><!UNSAFE_CALL!>.<!>funAny()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>nullableIntProperty<!>.funNullableT()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>nullableIntProperty<!>.funNullableAny()
}

/*
 * TESTCASE NUMBER: 18
 * ISSUES: KT-35668
 */
fun case_18(a: DeepObject.A.B.C.D.E.F.G.J?, b: Boolean) {
    val x = null
    val y = null

    if (a != (if (b) x else y) || x !== a) {
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
    val z = null
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
                            } else z
                        }
                    } else z
                }
            } else z
        }
    } else z

    if (a != z && a<!UNSAFE_CALL!>.<!>B19 !== z && a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19 != z && a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19 != z && a<!UNSAFE_CALL!>.<!>B19<!UNSAFE_CALL!>.<!>C19<!UNSAFE_CALL!>.<!>D19<!UNSAFE_CALL!>.<!>x !== z) {
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
fun case_20(x: Boolean, y: Nothing?) {
    val z = object {
        val B19 = object {
            val C19 = object {
                val D19 =  if (x) {
                    object {}
                } else y
            }
        }
    }

    if (z.B19.C19.D19 !== y) {
        z.B19.C19.D19
        z.B19.C19.D19<!UNSAFE_CALL!>.<!>equals(null)
        z.B19.C19.D19.propT
        z.B19.C19.D19<!UNSAFE_CALL!>.<!>propAny
        z.B19.C19.D19.propNullableT
        z.B19.C19.D19.propNullableAny
        z.B19.C19.D19.funT()
        z.B19.C19.D19<!UNSAFE_CALL!>.<!>funAny()
        z.B19.C19.D19.funNullableT()
        z.B19.C19.D19.funNullableAny()
    }
}

// TESTCASE NUMBER: 21
fun case_21() {
    if (EnumClassWithNullableProperty.A.prop_1 !== implicitNullableNothingProperty) {
        EnumClassWithNullableProperty.A.prop_1
        EnumClassWithNullableProperty.A.prop_1<!UNSAFE_CALL!>.<!>equals(null)
        EnumClassWithNullableProperty.A.prop_1.propT
        EnumClassWithNullableProperty.A.prop_1<!UNSAFE_CALL!>.<!>propAny
        EnumClassWithNullableProperty.A.prop_1.propNullableT
        EnumClassWithNullableProperty.A.prop_1.propNullableAny
        EnumClassWithNullableProperty.A.prop_1.funT()
        EnumClassWithNullableProperty.A.prop_1<!UNSAFE_CALL!>.<!>funAny()
        EnumClassWithNullableProperty.A.prop_1.funNullableT()
        EnumClassWithNullableProperty.A.prop_1.funNullableAny()
    }
}

// TESTCASE NUMBER: 22
fun case_22(a: (() -> Unit)?) {
    if (a != implicitNullableNothingProperty) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!UNSAFE_IMPLICIT_INVOKE_CALL!>a<!>()<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>a<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>a<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>a<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>a<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 23
fun case_23(a: ((Float) -> Int?)?, b: Float?, z: Nothing?) {
    if (a != z && b !== z && b !== z) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!><!UNSAFE_IMPLICIT_INVOKE_CALL!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float?")!>b<!>)<!>
        if (x != z || x !== implicitNullableNothingProperty) {
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

// TESTCASE NUMBER: 24
fun case_24(a: ((() -> Unit) -> Unit)?, b: (() -> Unit)?, z: Nothing?) =
    if (a !== z && b !== z) {
        <!UNSAFE_IMPLICIT_INVOKE_CALL!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>?")!>a<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>?")!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>?")!>a<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>?")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>?")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>?")!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>?")!>a<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>?")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>?")!>a<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!>.funNullableAny()
    } else z

// TESTCASE NUMBER: 25
fun case_25(b: Boolean, z: Nothing?) {
    val x = {
        if (b) object {
            val a = 10
        } else z
    }

    val y = if (b) x else z

    if (y !== z || y != implicitNullableNothingProperty) {
        val z1 = <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!><!UNSAFE_IMPLICIT_INVOKE_CALL!>y<!>()<!>

        if (z1 != z && implicitNullableNothingProperty !== z1) {
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>z1<!><!UNSAFE_CALL!>.<!>a
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>z1<!><!UNSAFE_CALL!>.<!>equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>z1<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>z1<!><!UNSAFE_CALL!>.<!>propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>z1<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>z1<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>z1<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>z1<!><!UNSAFE_CALL!>.<!>funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>z1<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>z1<!>.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 26
fun case_26(a: ((Float) -> Int?)?, b: Float?) {
    var z = null

    if (a != z == true && b != implicitNullableNothingProperty == true) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!><!UNSAFE_IMPLICIT_INVOKE_CALL!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float?")!>b<!>)<!>
        if (x != implicitNullableNothingProperty == true || z !== x) {
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
fun case_27(z: Nothing?) {
    if (Object.prop_1 == z == true == true == true == true == true == true == true == true == true == true == true == true == true == true)
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

// TESTCASE NUMBER: 28
fun case_28(a: DeepObject.A.B.C.D.E.F.G.J?) =
    if (a != implicitNullableNothingProperty == true == false == false == false == true == false == true == false == false == true == true) {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!><!UNSAFE_CALL!>.<!>x
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.funNullableAny()
    } else -1

/*
 * TESTCASE NUMBER: 29
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28328, KT-28329
 */
fun case_29(x: Boolean) {
    val v = null
    val z = {
        if (x) object {
            val a = 10
        } else null
    }

    val y = if (x) z else null

    if (false || false || false || false || y !== v) {
        val t = <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!><!UNSAFE_IMPLICIT_INVOKE_CALL!>y<!>()<!>

        if (<!EQUALITY_NOT_APPLICABLE_WARNING!>z !== t<!> || false) {
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>t<!><!UNSAFE_CALL!>.<!>a
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>t<!><!UNSAFE_CALL!>.<!>equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>t<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>t<!><!UNSAFE_CALL!>.<!>propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>t<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>t<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>t<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>t<!><!UNSAFE_CALL!>.<!>funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>t<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>t<!>.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 30
fun case_30(a: ((Float) -> Int?)?, b: Float?) {
    if (implicitNullableNothingProperty != a == true && b != implicitNullableNothingProperty == true || false || false || false || false || false || false || false || false || false) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!><!UNSAFE_IMPLICIT_INVOKE_CALL!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float?")!>b<!>)<!>
        if (false || implicitNullableNothingProperty != x == true) {
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

// TESTCASE NUMBER: 31
fun case_31(z1: Boolean?, z: Nothing?) {
    if (false || EnumClassWithNullableProperty.A.prop_1 != z && z1 !== z && z1) {
        EnumClassWithNullableProperty.A.prop_1
        EnumClassWithNullableProperty.A.prop_1<!UNSAFE_CALL!>.<!>equals(null)
        EnumClassWithNullableProperty.A.prop_1.propT
        EnumClassWithNullableProperty.A.prop_1<!UNSAFE_CALL!>.<!>propAny
        EnumClassWithNullableProperty.A.prop_1.propNullableT
        EnumClassWithNullableProperty.A.prop_1.propNullableAny
        EnumClassWithNullableProperty.A.prop_1.funT()
        EnumClassWithNullableProperty.A.prop_1<!UNSAFE_CALL!>.<!>funAny()
        EnumClassWithNullableProperty.A.prop_1.funNullableT()
        EnumClassWithNullableProperty.A.prop_1.funNullableAny()
    }
}

// TESTCASE NUMBER: 32
fun case_32(a: DeepObject.A.B.C.D.E.F.G.J?) =
    if (a == implicitNullableNothingProperty == true == false == false == false == true == false == true == false == false == true == true && true) {
        -1
    } else {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!><!UNSAFE_CALL!>.<!>x
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

// TESTCASE NUMBER: 33
fun case_33(a: ((Float) -> Int?)?, b: Float?, c: Boolean?) {
    var z = null

    if (true && a == z == true || b == null == true) {

    } else {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!><!UNSAFE_IMPLICIT_INVOKE_CALL!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float? & kotlin.Float")!>b<!>)<!>
        if (x == z == true && x === z || (c != z && <!UNSAFE_CALL!>!<!>c)) {

        } else {
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

/*
 * TESTCASE NUMBER: 34
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28329
 */
fun case_34(z1: Boolean?) {
    var z = null

    if (true && true && true && true && EnumClassWithNullableProperty.A.prop_1 != implicitNullableNothingProperty && EnumClassWithNullableProperty.A.prop_1 !== null && EnumClassWithNullableProperty.A.prop_1 !== z || z1 != implicitNullableNothingProperty || z1!! && true && true) {

    } else {
        EnumClassWithNullableProperty.A.prop_1
        EnumClassWithNullableProperty.A.prop_1<!UNSAFE_CALL!>.<!>equals(null)
        EnumClassWithNullableProperty.A.prop_1.propT
        EnumClassWithNullableProperty.A.prop_1<!UNSAFE_CALL!>.<!>propAny
        EnumClassWithNullableProperty.A.prop_1.propNullableT
        EnumClassWithNullableProperty.A.prop_1.propNullableAny
        EnumClassWithNullableProperty.A.prop_1.funT()
        EnumClassWithNullableProperty.A.prop_1<!UNSAFE_CALL!>.<!>funAny()
        EnumClassWithNullableProperty.A.prop_1.funNullableT()
        EnumClassWithNullableProperty.A.prop_1.funNullableAny()
    }
}

// TESTCASE NUMBER: 35
fun case_35(a: DeepObject.A.B.C.D.E.F.G.J?) {
    val itest = false

    if (true && a != implicitNullableNothingProperty && a !== implicitNullableNothingProperty || itest || !itest || true || !true) {

    } else {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.hashCode()
    }
}

/*
 * TESTCASE NUMBER: 36
 * UNEXPECTED BEHAVIOUR
 */
fun case_36(x: Any) {
    var z = null

    if (x == z) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.<!UNRESOLVED_REFERENCE!>java<!>
    }
}

// TESTCASE NUMBER: 37
fun case_37(x: Nothing?, y: Nothing?) {
    if (x == y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>.hashCode()
    }
}

/*
 * TESTCASE NUMBER: 38
 * UNEXPECTED BEHAVIOUR
 */
fun case_38() {
    val z = null

    if (Object.prop_2 != z)
    else {
        Object.prop_2
        Object.prop_2.<!UNRESOLVED_REFERENCE!>java<!>
    }
}

// TESTCASE NUMBER: 39
fun case_39(x: Char?) {
    if (x == implicitNullableNothingProperty && true) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char?")!>x<!>.hashCode()
    }
}

// TESTCASE NUMBER: 40
fun case_40() {
    val x: Unit? = null
    var z = null

    if (x == implicitNullableNothingProperty || z === x) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>.hashCode()
    }
}

// TESTCASE NUMBER: 41
fun case_41(x: EmptyClass?, z: Nothing?) {
    val y = true

    if (x === z && !y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass?")!>x<!>.hashCode()
    }
}

// TESTCASE NUMBER: 42
fun case_42() {
    if (EmptyObject == nullableNothingProperty || <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>EmptyObject<!> === nullableNothingProperty) {
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>EmptyObject<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>EmptyObject<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>EmptyObject<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>EmptyObject<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>EmptyObject<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>EmptyObject<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>EmptyObject<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>EmptyObject<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>EmptyObject<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>EmptyObject<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 43
fun case_43(x: TypealiasNullableString) {
    val z = null

    if (x == z && <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!> == z) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.hashCode()
    }
}

/*
 * TESTCASE NUMBER: 44
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28329
 */
fun case_44(x: TypealiasNullableString?, z1: Nothing?) {
    if (true && true && true && true && x !== z1) {

    } else if (false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 45
fun case_45() {
    val a = Class()
    var z: Nothing? = null

    if (a.prop_4 != z || true) {
        if (a.prop_4 == null) {
            a.prop_4
            a.prop_4.hashCode()
        }
    }
}

// TESTCASE NUMBER: 46
fun case_46(x: TypealiasNullableString?, y: TypealiasNullableString) {
    val t: TypealiasNullableString = null
    var z: Nothing? = null

    if (x != nullableNothingProperty) {

    } else {
        if (y === nullableNothingProperty) {
            if (z != nullableStringProperty) {
                if (z === t || t == nullableNothingProperty) {
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString?")!>x<!>.hashCode()
                }
            }
        }
    }
}

/*
 * TESTCASE NUMBER: 47
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28328
 */
fun case_47(x: TypealiasNullableString, y: TypealiasNullableString, z: Nothing?) = if (x !== z && true && true && true) "1"
    else if (y != z) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.funNullableAny()
    } else "-1"

// TESTCASE NUMBER: 48
fun case_48(x: EmptyClass12_48?, z: Nothing?) =
    if (x != z && true) {
        throw Exception()
    } else {
        <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.EmptyClass12_48?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.EmptyClass12_48?")!>x<!>.hashCode()
    }

// TESTCASE NUMBER: 49
class Case49 {
    val x: TypealiasNullableString?
    init {
        x = TypealiasNullableString()
    }
}

fun case_49() {
    val a = Case49()
    var z = null

    if (a.x === z) {
        a.x
        a.x.hashCode()
    }
}

// TESTCASE NUMBER: 50
fun case_50(x: TypealiasNullableString) {
    val z1 = null
    val z2 = null
    val t = if (x != z1 && z2 !== x) "" else {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString")!>x<!>.hashCode()
    }
}

// TESTCASE NUMBER: 51
fun case_51() {
    val x: TypealiasNullableNothing = null
    val z: Nothing? = null

    if (x === z || z == x && x == z || false || false || false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableNothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableNothing")!>x<!>.hashCode()
    }
}

// TESTCASE NUMBER: 52
val case_52 = if (nullableIntProperty !== nullableNothingProperty && nullableNothingProperty != nullableIntProperty) 0 else {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>nullableIntProperty<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>nullableIntProperty<!>.hashCode()
}

//TESTCASE NUMBER: 53
fun case_53(a: DeepObject.A.B.C.D.E.F.G.J?) {
    if (a == DeepObject.prop_2) {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.hashCode()
    }
}

// TESTCASE NUMBER: 54
fun case_54(b: Boolean) {
    val a = if (b) {
        object {
            var z = null
            val B54 = if (b) {
                object {
                    val C54 = if (b) {
                        object {
                            val D54 = if (b) {
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

    val z = null

    if (a != z && a<!UNSAFE_CALL!>.<!>B54 !== a<!UNSAFE_CALL!>.<!>z && a<!UNSAFE_CALL!>.<!>B54<!UNSAFE_CALL!>.<!>C54 != a<!UNSAFE_CALL!>.<!>z && a<!UNSAFE_CALL!>.<!>B54<!UNSAFE_CALL!>.<!>C54<!UNSAFE_CALL!>.<!>D54 != a<!UNSAFE_CALL!>.<!>z && a<!UNSAFE_CALL!>.<!>B54<!UNSAFE_CALL!>.<!>C54<!UNSAFE_CALL!>.<!>D54<!UNSAFE_CALL!>.<!>x === a<!UNSAFE_CALL!>.<!>z) {
        a<!UNSAFE_CALL!>.<!>B54<!UNSAFE_CALL!>.<!>C54<!UNSAFE_CALL!>.<!>D54<!UNSAFE_CALL!>.<!>x
        a<!UNSAFE_CALL!>.<!>B54<!UNSAFE_CALL!>.<!>C54<!UNSAFE_CALL!>.<!>D54<!UNSAFE_CALL!>.<!>x.hashCode()
    }
}

// TESTCASE NUMBER: 55
fun case_55(b: Boolean) {
    val a = object {
        val B19 = object {
            val C19 = object {
                var z = null
                val D19 =  if (b) {
                    object {}
                } else null
            }
        }
    }

    if (a.B19.C19.D19 === a.B19.C19.z) {
        a.B19.C19.D19
    }
}

// TESTCASE NUMBER: 56
fun case_56() {
    if (EnumClassWithNullableProperty.A.prop_1 == implicitNullableNothingProperty) {
        EnumClassWithNullableProperty.A.prop_1
        EnumClassWithNullableProperty.A.prop_1.hashCode()
    }
}

/*
 * TESTCASE NUMBER: 57
 * UNEXPECTED BEHAVIOUR
 */
fun case_57(a: (() -> Unit)) {
    var z = null

    if (a == z) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>")!>a<!>.<!UNRESOLVED_REFERENCE!>java<!>
    }
}

/*
 * TESTCASE NUMBER: 58
 * UNEXPECTED BEHAVIOUR
 */
fun case_58(a: ((Float) -> Int?)?, b: Float?, z: Nothing?) {
    if (a === z && b == z || z == a && z === b) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>?")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float?")!>b<!>
        if (a != z) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>?")!>a<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>?")!>a<!>.<!UNRESOLVED_REFERENCE!>java<!>
        }
    }
}

/*
 * TESTCASE NUMBER: 59
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28329
 */
fun case_59(a: ((() -> Unit) -> Unit)?, b: (() -> Unit)?, z: Nothing?) {
    if (false || false || a == z && b === z) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>?")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>?")!>a<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>?")!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>?")!>a<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>?")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>?")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>?")!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>?")!>a<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>?")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>?")!>a<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>?")!>b<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 60
fun case_60(b: Boolean) {
    val x = {
        if (b) object {
            val a = 10
        } else nullableNothingProperty
    }

    val y = if (b) x else nullableNothingProperty

    if (y != nullableNothingProperty) {
        val z = <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!><!UNSAFE_IMPLICIT_INVOKE_CALL!>y<!>()<!>

        if (z == nullableNothingProperty) {
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>z<!>
        }
    }
}

// TESTCASE NUMBER: 61
fun case_61(x: Any?) {
    if (x is Number?) {
        if (x !== implicitNullableNothingProperty) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number?")!>x<!><!UNSAFE_CALL!>.<!>equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number?")!>x<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number?")!>x<!><!UNSAFE_CALL!>.<!>propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number?")!>x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number?")!>x<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number?")!>x<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number?")!>x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number?")!>x<!>.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 62
fun case_62(x: Any?) {
    var z = null
    if (x is Number? && x is Int? && x != z) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int?")!>x<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int?")!>x<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 63
fun case_63(x: Any?, b: Boolean) {
    val z1 = null
    val z2 = null
    val z3 = null

    if (x is Number?) {
        if (x !== when (b) { true -> z1; false -> z2; else -> z3 }) {
            if (x is Int?) {
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int?")!>x<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int?")!>x<!><!UNSAFE_CALL!>.<!>equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int?")!>x<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int?")!>x<!><!UNSAFE_CALL!>.<!>propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int?")!>x<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int?")!>x<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int?")!>x<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int?")!>x<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int?")!>x<!>.funNullableAny()
            }
        }
    }
}

// TESTCASE NUMBER: 64
fun case_64(x: Any?) {
    if (x != try {implicitNullableNothingProperty} finally {}) {
        if (x is Number) {
            if (x is Int?) {
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>x<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>x<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>x<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>x<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>x<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>x<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>x<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>x<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>x<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>x<!>.funNullableAny()
            }
        }
    }
}

// TESTCASE NUMBER: 65
fun case_65(x: Any?, z: Nothing?) {
    if (x is ClassLevel1?) {
        if (x is ClassLevel2?) {
            if (x is ClassLevel3?) {
                if (x is ClassLevel4?) {
                    if (x is ClassLevel5?) {
                        if (x != z) {
                            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>
                            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!><!UNSAFE_CALL!>.<!>equals(null)
                            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.propT
                            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!><!UNSAFE_CALL!>.<!>propAny
                            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.propNullableT
                            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.propNullableAny
                            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.funT()
                            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
                            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.funNullableT()
                            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.funNullableAny()
                        }
                    }
                }
            }
        }
    }
}

/*
 * TESTCASE NUMBER: 66
 * ISSUES: KT-35668
 */
fun case_66(x: Any?, z1: Nothing?, z2: Nothing?, b: Boolean) {
    if (x is ClassLevel1?) {
        if (x is ClassLevel2?) {
            if (x is ClassLevel3?) {
                if (x != if (b) { z1 } else { z2 } && x is ClassLevel4?) {
                    if (x is ClassLevel5?) {
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!><!UNSAFE_CALL!>.<!>equals(null)
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.propT
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!><!UNSAFE_CALL!>.<!>propAny
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.propNullableT
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.propNullableAny
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.funT()
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.funNullableT()
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.funNullableAny()
                    }
                }
            }
        }
    }
}

// TESTCASE NUMBER: 67
fun case_67(x: Any?) {
    var z = null

    if (x is ClassLevel1? && x is ClassLevel2? && x is ClassLevel3?) {
        if (x is ClassLevel4? && x != (fun (): Nothing? { return z })() && x is ClassLevel5?) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!><!UNSAFE_CALL!>.<!>equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!><!UNSAFE_CALL!>.<!>propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 68
fun case_68(x: Any?, z: Nothing?) {
    if (x is ClassLevel1? && x is ClassLevel2? && x is ClassLevel3?) {
        if (x is ClassLevel4? && x != (fun (): Nothing? { return z })() && x is ClassLevel5?) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!><!UNSAFE_CALL!>.<!>equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!><!UNSAFE_CALL!>.<!>propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.funNullableAny()
        }
    }
}

/*
 * TESTCASE NUMBER: 69
 * ISSUES: KT-35668
 */
fun case_69(x: Any?, z: Nothing?) {
    if (x is ClassLevel1? && x is ClassLevel2? && x is ClassLevel3? && x is ClassLevel4? && x != try { z } catch (e: Exception) { z } && x is ClassLevel5?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 70
fun case_70(x: Any?) {
    if (x is ClassLevel1? && x is ClassLevel2? && x is ClassLevel3?) {
        if (x is ClassLevel4?) {

        } else if (x is ClassLevel5? && x != nullableNothingProperty || x != implicitNullableNothingProperty) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel3?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel3?")!>x<!><!UNSAFE_CALL!>.<!>equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel3?")!>x<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel3?")!>x<!><!UNSAFE_CALL!>.<!>propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel3?")!>x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel3?")!>x<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel3?")!>x<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel3?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel3?")!>x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel3?")!>x<!>.funNullableAny()
        }
    } else if (x is ClassLevel4? && x !== nullableNothingProperty && x is ClassLevel5?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5?")!>x<!>.funNullableAny()
    }
}

/*
 * TESTCASE NUMBER: 71
 * NOTE: lazy smartcasts
 * DISCUSSION
 * ISSUES: KT-28362
 */
fun case_71(t: Any?) {
    val z1 = null
    var z2 = z1

    if (t is Interface1?) {
        if (t is Interface2?) {
            if (t != z2) {
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Interface1? & Interface2?")!>t<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Interface1? & Interface2?")!>t<!><!UNSAFE_CALL!>.<!>itest1()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Interface1? & Interface2?")!>t<!><!UNSAFE_CALL!>.<!>itest2()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Interface1? & Interface2?")!>t<!><!UNSAFE_CALL!>.<!>itest()

                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Interface1? & Interface2?")!>t<!>.let { <!DEBUG_INFO_EXPRESSION_TYPE("Interface1? & Interface2?")!>it<!><!UNSAFE_CALL!>.<!>itest1(); <!DEBUG_INFO_EXPRESSION_TYPE("Interface1? & Interface2?")!>it<!><!UNSAFE_CALL!>.<!>itest2() }
            }
        }
    }
}

/*
 * TESTCASE NUMBER: 72
 * NOTE: lazy smartcasts
 * DISCUSSION
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28362, KT-27032, KT-35668
 */
fun case_72(t: Any?, z1: Nothing?) {
    var z2 = null

    if (t is Interface1? && t != z1 ?: z2 && t is Interface2?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Interface1? & Interface2?")!>t<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Interface1? & Interface2?")!>t<!><!UNSAFE_CALL!>.<!>itest1()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Interface1? & Interface2?")!>t<!><!UNSAFE_CALL!>.<!>itest2()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Interface1? & Interface2?")!>t<!><!UNSAFE_CALL!>.<!>itest()

        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Interface1? & Interface2?")!>t<!>.let { <!DEBUG_INFO_EXPRESSION_TYPE("Interface1? & Interface2?")!>it<!><!UNSAFE_CALL!>.<!>itest1(); <!DEBUG_INFO_EXPRESSION_TYPE("Interface1? & Interface2?")!>it<!><!UNSAFE_CALL!>.<!>itest2() }
    }
}

/*
 * TESTCASE NUMBER: 73
 * NOTE: lazy smartcasts
 * DISCUSSION
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28362
 */
fun case_73(t: Any?) {
    val `null` = null

    if (t is Interface2?) {
        if (t is ClassLevel1?) {
            if (t is ClassLevel2? && t is Interface1?) {
                if (t !is Interface3?) {} else if (false) {
                    if (t != `null`) {
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Interface2? & ClassLevel2? & Interface1? & Interface3?")!>t<!><!UNSAFE_CALL!>.<!>itest2()
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Interface2? & ClassLevel2? & Interface1? & Interface3?")!>t<!><!UNSAFE_CALL!>.<!>itest1()
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Interface2? & ClassLevel2? & Interface1? & Interface3?")!>t<!><!UNSAFE_CALL!>.<!>itest()
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Interface2? & ClassLevel2? & Interface1? & Interface3?")!>t<!><!UNSAFE_CALL!>.<!>test1()
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Interface2? & ClassLevel2? & Interface1? & Interface3?")!>t<!><!UNSAFE_CALL!>.<!>test2()
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Interface2? & ClassLevel2? & Interface1? & Interface3?")!>t<!>
                    }
                }
            }
        }
    }
}

/*
 * TESTCASE NUMBER: 74
 * NOTE: lazy smartcasts
 * DISCUSSION
 * ISSUES: KT-28362
 */
fun case_74(t: Any?) {
    if (t is Interface2?) {
        if (t is ClassLevel1?) {
            if (t == implicitNullableNothingProperty || t === implicitNullableNothingProperty || t !is Interface1?) else {
                if (t is ClassLevel2?) {
                    if (t is Interface3?) {
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Interface2? & Interface1? & ClassLevel2? & Interface3?")!>t<!><!UNSAFE_CALL!>.<!>itest2()
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Interface2? & Interface1? & ClassLevel2? & Interface3?")!>t<!><!UNSAFE_CALL!>.<!>itest1()
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Interface2? & Interface1? & ClassLevel2? & Interface3?")!>t<!><!UNSAFE_CALL!>.<!>itest()
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Interface2? & Interface1? & ClassLevel2? & Interface3?")!>t<!><!UNSAFE_CALL!>.<!>test1()
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Interface2? & Interface1? & ClassLevel2? & Interface3?")!>t<!><!UNSAFE_CALL!>.<!>test2()
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Interface2? & Interface1? & ClassLevel2? & Interface3?")!>t<!>
                    }
                }
            }
        }
    }
}

/*
 * TESTCASE NUMBER: 75
 * NOTE: lazy smartcasts
 * DISCUSSION
 * ISSUES: KT-28362
 */
fun case_75(t: Any?, z: Nothing?) {
    if (t !is ClassLevel2? || t !is ClassLevel1?) else {
        if (t === ((((((z)))))) || t !is Interface1?) else {
            if (t !is Interface2? || t !is Interface3?) {} else {
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel2? & Interface1? & Interface2? & Interface3?")!>t<!><!UNSAFE_CALL!>.<!>itest2()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel2? & Interface1? & Interface2? & Interface3?")!>t<!><!UNSAFE_CALL!>.<!>itest1()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel2? & Interface1? & Interface2? & Interface3?")!>t<!><!UNSAFE_CALL!>.<!>itest()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel2? & Interface1? & Interface2? & Interface3?")!>t<!><!UNSAFE_CALL!>.<!>test1()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel2? & Interface1? & Interface2? & Interface3?")!>t<!><!UNSAFE_CALL!>.<!>test2()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel2? & Interface1? & Interface2? & Interface3?")!>t<!>
            }
        }
    }
}

// TESTCASE NUMBER: 76
fun case_76(a: Any?, b: Int = if (a !is Number? === true || a !is Int? == true || a != null == false == true) 0 else <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>a<!>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>b<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>b<!>.equals(null)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>b<!>.propT
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>b<!>.propAny
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>b<!>.propNullableT
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>b<!>.propNullableAny
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>b<!>.funT()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>b<!>.funAny()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>b<!>.funNullableT()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>b<!>.funNullableAny()
}
