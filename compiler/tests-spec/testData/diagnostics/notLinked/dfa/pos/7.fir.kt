// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNREACHABLE_CODE -CAN_BE_VAL
// SKIP_TXT
// WITH_EXTENDED_CHECKERS

// FILE: other_package.kt

package orherpackage

// TESTCASE NUMBER: 13
class EmptyClass13 {}

// TESTCASE NUMBER: 14
typealias TypealiasString14 = String?

// FILE: main.kt

import orherpackage.*

// TESTCASE NUMBER: 1
fun case_1(x: Any?) {
    if (x != null || <!SENSELESS_COMPARISON!>x != null<!> || <!SENSELESS_COMPARISON!>x != null<!> || <!SENSELESS_COMPARISON!>x != null<!> || <!SENSELESS_COMPARISON!>x != null<!> || <!SENSELESS_COMPARISON!>x != null<!> || <!SENSELESS_COMPARISON!>x != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>x<!>.funNullableAny()
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28159
 */
fun case_2(x: Nothing?) {
    if (<!SENSELESS_COMPARISON!>x !== null<!> && <!SENSELESS_COMPARISON!>x !== null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
    }
}

// TESTCASE NUMBER: 3
fun case_3() {
    if (Object.prop_1 == null && <!SENSELESS_COMPARISON!>Object.prop_1 == null<!>)
    else {
        Object.prop_1
        Object.prop_1.equals(null)
        Object.prop_1.propT
        Object.prop_1.propAny
        Object.prop_1.propNullableT
        Object.prop_1.propNullableAny
        Object.prop_1.funT()
        Object.prop_1.funAny()
        Object.prop_1.funNullableT()
        Object.prop_1.funNullableAny()
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: Char?) {
    if (x != null && true || x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char? & kotlin.Char")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char? & kotlin.Char")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char? & kotlin.Char")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char? & kotlin.Char")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char? & kotlin.Char")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char? & kotlin.Char")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char? & kotlin.Char")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char? & kotlin.Char")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char? & kotlin.Char")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char? & kotlin.Char")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 5
fun case_5() {
    val x: Unit? = null

    if (x !== null || <!SENSELESS_COMPARISON!>x !== null<!> && <!SENSELESS_COMPARISON!>x !== null<!> || <!SENSELESS_COMPARISON!>x !== null<!> && <!SENSELESS_COMPARISON!>x !== null<!>) {
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
fun case_6(x: Class?) {
    val y = true

    if (((false || x != null || false) && !y) || x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 7
fun case_7() {
    val x: EmptyObject? = null
    if (x != null || <!SENSELESS_COMPARISON!>x != null<!> || <!SENSELESS_COMPARISON!><!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject? & kotlin.Nothing?")!>x<!> != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject? & EmptyObject")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject? & EmptyObject")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject? & EmptyObject")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject? & EmptyObject")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject? & EmptyObject")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject? & EmptyObject")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject? & EmptyObject")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject? & EmptyObject")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject? & EmptyObject")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject? & EmptyObject")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 8
fun case_8(x: TypealiasString) {
    if (<!SENSELESS_COMPARISON!>x !== null<!> && <!SENSELESS_COMPARISON!><!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString")!>x<!> != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 9
fun case_9(x: TypealiasNullableString<!REDUNDANT_NULLABLE!>?<!>) {
    if (x === null && <!SENSELESS_COMPARISON!>x === null<!> || <!SENSELESS_COMPARISON!>x === null<!>) {

    } else if (<!SENSELESS_COMPARISON!>x === null<!> || <!SENSELESS_COMPARISON!>x === null<!>) {
    } else if (false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? & kotlin.String")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? & kotlin.String")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? & kotlin.String")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? & kotlin.String")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? & kotlin.String")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? & kotlin.String")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? & kotlin.String")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? & kotlin.String")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? & kotlin.String")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? & kotlin.String")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 10
fun case_10() {
    val a = Class()

    if (<!FORBIDDEN_IDENTITY_EQUALS_WARNING!>a.prop_4 === null<!> || <!FORBIDDEN_IDENTITY_EQUALS_WARNING, SENSELESS_COMPARISON!>a.prop_4 === null<!> || true) {
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
fun case_11(x: TypealiasNullableString<!REDUNDANT_NULLABLE!>?<!>, y: TypealiasNullableString) {
    val t: TypealiasNullableString = null

    if (x == null) {

    } else {
        if (<!SENSELESS_COMPARISON!>x != null<!>) {
            if (y != null || <!SENSELESS_COMPARISON!>y != null<!>) {
                if (<!SENSELESS_COMPARISON!>stringProperty == null<!> && <!SENSELESS_COMPARISON!>nullableNothingProperty == null<!>) {
                    if (t != null || <!SENSELESS_COMPARISON!>t != null<!>) {
                        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? & kotlin.String")!>x<!>
                        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? & kotlin.String")!>x<!>.equals(null)
                        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? & kotlin.String")!>x<!>.propT
                        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? & kotlin.String")!>x<!>.propAny
                        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? & kotlin.String")!>x<!>.propNullableT
                        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? & kotlin.String")!>x<!>.propNullableAny
                        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? & kotlin.String")!>x<!>.funT()
                        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? & kotlin.String")!>x<!>.funAny()
                        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? & kotlin.String")!>x<!>.funNullableT()
                        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? & kotlin.String")!>x<!>.funNullableAny()
                    }
                }
            }
        }
    }
}

// TESTCASE NUMBER: 12
fun case_12(x: TypealiasNullableString, y: TypealiasNullableString) = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>if (x == null) "1"
    else if (y === null || <!SENSELESS_COMPARISON!>y === null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString & kotlin.String")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString & kotlin.String")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString & kotlin.String")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString & kotlin.String")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString & kotlin.String")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString & kotlin.String")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString & kotlin.String")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString & kotlin.String")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString & kotlin.String")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString & kotlin.String")!>x<!>
    } else "-1"<!>

// TESTCASE NUMBER: 13
fun case_13(x: orherpackage.EmptyClass13?, y: Nothing?) =
    <!DEBUG_INFO_EXPRESSION_TYPE("orherpackage.EmptyClass13")!>if (x == null || <!SENSELESS_COMPARISON!>x === y<!>) {
        throw Exception()
    } else {
        <!DEBUG_INFO_EXPRESSION_TYPE("orherpackage.EmptyClass13? & orherpackage.EmptyClass13")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("orherpackage.EmptyClass13? & orherpackage.EmptyClass13")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("orherpackage.EmptyClass13? & orherpackage.EmptyClass13")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("orherpackage.EmptyClass13? & orherpackage.EmptyClass13")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("orherpackage.EmptyClass13? & orherpackage.EmptyClass13")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("orherpackage.EmptyClass13? & orherpackage.EmptyClass13")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("orherpackage.EmptyClass13? & orherpackage.EmptyClass13")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("orherpackage.EmptyClass13? & orherpackage.EmptyClass13")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("orherpackage.EmptyClass13? & orherpackage.EmptyClass13")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("orherpackage.EmptyClass13? & orherpackage.EmptyClass13")!>x<!>
    }<!>

// TESTCASE NUMBER: 14
fun case_14() {
    val a = Class()
    if (<!SENSELESS_COMPARISON!>a.prop_6 != a.prop_7<!>) {
        a.prop_6
        a.prop_6.equals(null)
        a.prop_6.propT
        a.prop_6.propAny
        a.prop_6.propNullableT
        a.prop_6.propNullableAny
        a.prop_6.funT()
        a.prop_6.funAny()
        a.prop_6.funNullableT()
        a.prop_6.funNullableAny()
    }
}

// TESTCASE NUMBER: 15
fun case_15(x: TypealiasString?) {
    var y = null
    val t = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>if (x === null || <!SENSELESS_COMPARISON!>x == y<!> && <!SENSELESS_COMPARISON!>x === y<!>) "" else {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString? & kotlin.String")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString? & kotlin.String")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString? & kotlin.String")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString? & kotlin.String")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString? & kotlin.String")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString? & kotlin.String")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString? & kotlin.String")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString? & kotlin.String")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString? & kotlin.String")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString? & kotlin.String")!>x<!>
    }<!>
}

// TESTCASE NUMBER: 16
fun case_16() {
    val x: TypealiasNothing? = null
    val y: Nothing? = null

    if (<!SENSELESS_COMPARISON!>x != null<!> || <!SENSELESS_COMPARISON!>x !== null<!> || <!SENSELESS_COMPARISON!>x != y<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNothing? & kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNothing? & kotlin.Nothing")!>x<!>.hashCode()
    }
}

// TESTCASE NUMBER: 17
val case_17 = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>if (nullableIntProperty == null || <!FORBIDDEN_IDENTITY_EQUALS_WARNING, SENSELESS_COMPARISON!>nullableNothingProperty === nullableIntProperty<!>) 0 else {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>nullableIntProperty<!>.equals(null)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>nullableIntProperty<!>.propT
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>nullableIntProperty<!>.propAny
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>nullableIntProperty<!>.propNullableT
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>nullableIntProperty<!>.propNullableAny
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>nullableIntProperty<!>.funT()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>nullableIntProperty<!>.funAny()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>nullableIntProperty<!>.funNullableT()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>nullableIntProperty<!>.funNullableAny()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>nullableIntProperty<!>
}<!>

/*
 * TESTCASE NUMBER: 18
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28328
 */
fun case_18(a: DeepObject.A.B.C.D.E.F.G.J?, b: Nothing?) {
    if (a != null || <!SENSELESS_COMPARISON!>b !== a<!> || false) {
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
            var y = null
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

    if (a != null && a.B19 != a.y && a.B19.C19 != a.y && a.B19.C19.D19 != a.y && a.B19.C19.D19.x != a.y) {
        a.B19.C19.D19.x
        a.B19.C19.D19.x.equals(null)
        a.B19.C19.D19.x.propT
        a.B19.C19.D19.x.propAny
        a.B19.C19.D19.x.propNullableT
        a.B19.C19.D19.x.propNullableAny
        a.B19.C19.D19.x.funT()
        a.B19.C19.D19.x.funAny()
        a.B19.C19.D19.x.funNullableT()
        a.B19.C19.D19.x.funNullableAny()
    }
}

// TESTCASE NUMBER: 20
fun case_20(b: Boolean) {
    val a = object {
        val y = null
        val B19 = object {
            val C19 = object {
                val D19 =  if (b) {
                    object {}
                } else null
            }
        }
    }

    if (a.B19.C19.D19 !== null || <!SENSELESS_COMPARISON!>a.y != a.B19.C19.D19<!>) {
        a.B19.C19.D19
        a.B19.C19.D19.equals(null)
        a.B19.C19.D19.propT
        a.B19.C19.D19.propAny
        a.B19.C19.D19.propNullableT
        a.B19.C19.D19.propNullableAny
        a.B19.C19.D19.funT()
        a.B19.C19.D19.funAny()
        a.B19.C19.D19.funNullableT()
        a.B19.C19.D19.funNullableAny()
    }
}

// TESTCASE NUMBER: 21
fun case_21() {
    val y = null
    if (<!FORBIDDEN_IDENTITY_EQUALS_WARNING!>EnumClassWithNullableProperty.A.prop_1 !== null<!> && <!SENSELESS_COMPARISON!>y != EnumClassWithNullableProperty.A.prop_1<!>) {
        EnumClassWithNullableProperty.A.prop_1
        EnumClassWithNullableProperty.A.prop_1.equals(null)
        EnumClassWithNullableProperty.A.prop_1.propT
        EnumClassWithNullableProperty.A.prop_1.propAny
        EnumClassWithNullableProperty.A.prop_1.propNullableT
        EnumClassWithNullableProperty.A.prop_1.propNullableAny
        EnumClassWithNullableProperty.A.prop_1.funT()
        EnumClassWithNullableProperty.A.prop_1.funAny()
        EnumClassWithNullableProperty.A.prop_1.funNullableT()
        EnumClassWithNullableProperty.A.prop_1.funNullableAny()
    }
}

// TESTCASE NUMBER: 22
fun case_22(a: (() -> Unit)?) {
    var y = null
    if (a != null || <!SENSELESS_COMPARISON!>y != a<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>a()<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>? & kotlin.Function0<kotlin.Unit>")!>a<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>? & kotlin.Function0<kotlin.Unit>")!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>? & kotlin.Function0<kotlin.Unit>")!>a<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>? & kotlin.Function0<kotlin.Unit>")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>? & kotlin.Function0<kotlin.Unit>")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>? & kotlin.Function0<kotlin.Unit>")!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>? & kotlin.Function0<kotlin.Unit>")!>a<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>? & kotlin.Function0<kotlin.Unit>")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>? & kotlin.Function0<kotlin.Unit>")!>a<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 23
fun case_23(a: ((Float) -> Int?)?, b: Float?, c: Nothing?) {
    if (a != null && <!FORBIDDEN_IDENTITY_EQUALS_WARNING!>b !== null<!> || a != c && <!FORBIDDEN_IDENTITY_EQUALS_WARNING!>b !== c<!>) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float? & kotlin.Float")!>b<!>)<!>
        if (x != null || <!FORBIDDEN_IDENTITY_EQUALS_WARNING, SENSELESS_COMPARISON!>c !== x<!>) {
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
    if (a !== null && b !== null || implicitNullableNothingProperty != a && nullableNothingProperty !== b) {
        a(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>? & kotlin.Function0<kotlin.Unit>")!>b<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>? & kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>")!>a<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>? & kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>")!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>? & kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>")!>a<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>? & kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>? & kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>? & kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>")!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>? & kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>")!>a<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>? & kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>? & kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>")!>a<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>? & kotlin.Function0<kotlin.Unit>")!>b<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>? & kotlin.Function0<kotlin.Unit>")!>b<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>? & kotlin.Function0<kotlin.Unit>")!>b<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>? & kotlin.Function0<kotlin.Unit>")!>b<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>? & kotlin.Function0<kotlin.Unit>")!>b<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>? & kotlin.Function0<kotlin.Unit>")!>b<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>? & kotlin.Function0<kotlin.Unit>")!>b<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>? & kotlin.Function0<kotlin.Unit>")!>b<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>? & kotlin.Function0<kotlin.Unit>")!>b<!>.funNullableAny()
    } else null

// TESTCASE NUMBER: 25
fun case_25(b: Boolean) {
    val x = {
        if (b) object {
            val a = 10
            val b = null
        } else null
    }

    val y = if (b) x else null

    if (y !== null || <!SENSELESS_COMPARISON!>x()!!.b != y<!>) {
        if (<!SENSELESS_COMPARISON!>x()!!.b != y<!>) {
            val z = <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>y()<!>

            if (z != null) {
                <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>? & <anonymous>")!>z<!>.a
                <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>? & <anonymous>")!>z<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>? & <anonymous>")!>z<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>? & <anonymous>")!>z<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>? & <anonymous>")!>z<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>? & <anonymous>")!>z<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>? & <anonymous>")!>z<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>? & <anonymous>")!>z<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>? & <anonymous>")!>z<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>? & <anonymous>")!>z<!>.funNullableAny()
            }
        }
    }
}

// TESTCASE NUMBER: 26
fun case_26(a: ((Float) -> Int?)?, b: Float?) {
    var c: Nothing? = null

    if (a != null == true && b != null == true || c != a == true && b != c == true) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float? & kotlin.Float")!>b<!>)<!>
        if (x != null == true) {
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

// TESTCASE NUMBER: 27
fun case_27(y: Nothing?) {
    if (Object.prop_1 == null == true == true == true == true == true == true == true == true == true == true == true == true == true == true || <!SENSELESS_COMPARISON!>y == Object.prop_1<!> == true == true == true == false == false)
    else {
        Object.prop_1
        Object.prop_1.equals(null)
        Object.prop_1.propT
        Object.prop_1.propAny
        Object.prop_1.propNullableT
        Object.prop_1.propNullableAny
        Object.prop_1.funT()
        Object.prop_1.funAny()
        Object.prop_1.funNullableT()
        Object.prop_1.funNullableAny()
    }
}

//TESTCASE NUMBER: 28
fun case_28(a: DeepObject.A.B.C.D.E.F.G.J?) =
    if (a != null == true == false == false == false == true == false == true == false == false == true == true && <!FORBIDDEN_IDENTITY_EQUALS_WARNING!>a.x !== nullableNothingProperty<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J? & DeepObject.A.B.C.D.E.F.G.J")!>a<!>.x
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J? & DeepObject.A.B.C.D.E.F.G.J")!>a<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J? & DeepObject.A.B.C.D.E.F.G.J")!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J? & DeepObject.A.B.C.D.E.F.G.J")!>a<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J? & DeepObject.A.B.C.D.E.F.G.J")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J? & DeepObject.A.B.C.D.E.F.G.J")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J? & DeepObject.A.B.C.D.E.F.G.J")!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J? & DeepObject.A.B.C.D.E.F.G.J")!>a<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J? & DeepObject.A.B.C.D.E.F.G.J")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J? & DeepObject.A.B.C.D.E.F.G.J")!>a<!>.funNullableAny()
    } else -1

// TESTCASE NUMBER: 29
fun case_29(a: Int?, b: Nothing?, c: Int = if (<!FORBIDDEN_IDENTITY_EQUALS_WARNING!>a === b<!>) 0 else a) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>b<!>
}

// TESTCASE NUMBER: 30
fun case_30(a: Int?, b: Nothing?, c: Int = if (<!FORBIDDEN_IDENTITY_EQUALS_WARNING!>a === b<!> || <!SENSELESS_COMPARISON!>a == null<!>) 0 else a) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>b<!>
}
