// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 6
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, enumClasses, interfaces, objects, typealiases, properties, functions
 */

// FILE: other_types.kt

package othertypes

// TESTCASE NUMBER: 12, 48
class EmptyClass12_48 {}

// FILE: main.kt

import othertypes.*

// TESTCASE NUMBER: 1
fun case_1(x: Any?) {
    val y = null
    if (x != <!DEBUG_INFO_CONSTANT!>y<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>.funNullableAny()
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28159
 */
fun case_2(x: Nothing?) {
    val y = null
    if (<!DEBUG_INFO_CONSTANT!>x<!> !== <!DEBUG_INFO_CONSTANT!>y<!>) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>.hashCode()
    }
}

// TESTCASE NUMBER: 3
fun case_3() {
    val y = null

    if (Object.prop_1 == <!DEBUG_INFO_CONSTANT!>y<!>)
    else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>Object.prop_1<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!>Object.prop_1<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!>Object.prop_1<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!>Object.prop_1<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>Object.prop_1<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>Object.prop_1<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!>Object.prop_1<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!>Object.prop_1<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>Object.prop_1<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>Object.prop_1<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: Char?, y: Nothing?) {
    if (x != <!DEBUG_INFO_CONSTANT!>y<!> && true) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 5
fun case_5() {
    val x: Unit? = null
    val y: Nothing? = null

    if (x !== <!DEBUG_INFO_CONSTANT!>y<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 6
fun case_6(x: EmptyClass?, z: Nothing?) {
    val y = true

    if (x != <!DEBUG_INFO_CONSTANT!>z<!> && !y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass & EmptyClass?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass & EmptyClass?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass & EmptyClass?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass & EmptyClass?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass & EmptyClass?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass & EmptyClass?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass & EmptyClass?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass & EmptyClass?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass & EmptyClass?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass & EmptyClass?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 7
fun case_7(x: EmptyObject?) {
    val y = null

    if (x != <!DEBUG_INFO_CONSTANT!>y<!> || <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("EmptyObject? & kotlin.Nothing?")!>x<!> != <!DEBUG_INFO_CONSTANT!>y<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject & EmptyObject?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject & EmptyObject?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject & EmptyObject?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject & EmptyObject?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject & EmptyObject?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject & EmptyObject?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject & EmptyObject?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject & EmptyObject?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject & EmptyObject?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject & EmptyObject?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 8
fun case_8(x: TypealiasNullableString) {
    val y = null

    if (x !== <!DEBUG_INFO_CONSTANT!>y<!> && <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */")!>x<!> != <!DEBUG_INFO_CONSTANT!>y<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 9
fun case_9(x: TypealiasNullableString<!REDUNDANT_NULLABLE!>?<!>, y: Nothing?) {
    if (x === <!DEBUG_INFO_CONSTANT!>y<!>) {

    } else if (false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 10
fun case_10() {
    val a = Class()
    val b = null

    if (a.prop_4 === <!DEBUG_INFO_CONSTANT!>b<!> || true) {
        if (a.prop_4 != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>a.prop_4<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float"), DEBUG_INFO_SMARTCAST!>a.prop_4<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float"), DEBUG_INFO_SMARTCAST!>a.prop_4<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float"), DEBUG_INFO_SMARTCAST!>a.prop_4<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>a.prop_4<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>a.prop_4<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float"), DEBUG_INFO_SMARTCAST!>a.prop_4<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float"), DEBUG_INFO_SMARTCAST!>a.prop_4<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>a.prop_4<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>a.prop_4<!>.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 11
fun case_11(x: TypealiasNullableString<!REDUNDANT_NULLABLE!>?<!>, y: TypealiasNullableString) {
    val z = null
    val u: TypealiasNullableString = null
    val v = null

    if (x == <!DEBUG_INFO_CONSTANT!>z<!> && <!DEBUG_INFO_CONSTANT!>x<!> == <!DEBUG_INFO_CONSTANT!>v<!>) {

    } else {
        if (y != <!DEBUG_INFO_CONSTANT!>z<!>) {
            if (nullableStringProperty == <!DEBUG_INFO_CONSTANT!>z<!>) {
                if (u != <!DEBUG_INFO_CONSTANT!>z<!> || <!DEBUG_INFO_CONSTANT!>u<!> != <!DEBUG_INFO_CONSTANT!>v<!>) {
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */")!>x<!>
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.propT
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */")!>x<!>.propNullableT
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */")!>x<!>.propNullableAny
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */")!>x<!>.funNullableT()
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */")!>x<!>.funNullableAny()
                }
            }
        }
    }
}

// TESTCASE NUMBER: 12
fun case_12(x: TypealiasNullableString, y: TypealiasNullableString, z1: Nothing?, z2: Nothing?) = if (x == <!DEBUG_INFO_CONSTANT!>z1<!> || x == <!DEBUG_INFO_CONSTANT!>z2<!>) "1"
    else if (y === <!DEBUG_INFO_CONSTANT!>z1<!> && <!DEBUG_INFO_CONSTANT!>y<!> == <!DEBUG_INFO_CONSTANT!>z2<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */")!>x<!>.funNullableAny()
    } else "-1"

// TESTCASE NUMBER: 13
fun case_13(x: EmptyClass12_48?, z: Nothing?) =
    if (x == <!DEBUG_INFO_CONSTANT!>z<!> || x === <!DEBUG_INFO_CONSTANT!>z<!> && x == <!DEBUG_INFO_CONSTANT!>z<!>) {
        throw Exception()
    } else {
        <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.EmptyClass12_48 & othertypes.EmptyClass12_48?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.EmptyClass12_48 & othertypes.EmptyClass12_48?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.EmptyClass12_48 & othertypes.EmptyClass12_48?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.EmptyClass12_48 & othertypes.EmptyClass12_48?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.EmptyClass12_48 & othertypes.EmptyClass12_48?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.EmptyClass12_48 & othertypes.EmptyClass12_48?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.EmptyClass12_48 & othertypes.EmptyClass12_48?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.EmptyClass12_48 & othertypes.EmptyClass12_48?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.EmptyClass12_48 & othertypes.EmptyClass12_48?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.EmptyClass12_48 & othertypes.EmptyClass12_48?")!>x<!>.funNullableAny()
    }

// TESTCASE NUMBER: 14
class Case14 {
    val x: TypealiasNullableString<!REDUNDANT_NULLABLE!>?<!>
    init {
        x = TypealiasNullableString()
    }
}

fun case_14() {
    val a = Case14()
    val x = null
    val y = <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!>

    if (a.x != <!DEBUG_INFO_CONSTANT!>x<!> && a.x != <!DEBUG_INFO_CONSTANT!>y<!> || a.x != <!DEBUG_INFO_CONSTANT!>y<!> && <!SENSELESS_COMPARISON!>a.x !== null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */")!>a.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */"), DEBUG_INFO_SMARTCAST!>a.x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */"), DEBUG_INFO_SMARTCAST!>a.x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */"), DEBUG_INFO_SMARTCAST!>a.x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */")!>a.x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */")!>a.x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */"), DEBUG_INFO_SMARTCAST!>a.x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */"), DEBUG_INFO_SMARTCAST!>a.x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */")!>a.x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */")!>a.x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 15
fun case_15(x: TypealiasNullableString) {
    val y = null
    val <!UNUSED_VARIABLE!>z<!> = if (x === null || <!DEBUG_INFO_CONSTANT!>y<!> == x && x === <!DEBUG_INFO_CONSTANT!>y<!> || <!SENSELESS_COMPARISON!>null === x<!>) "" else {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 16
fun case_16() {
    val x: TypealiasNullableNothing = null
    val y: Nothing? = null

    if (<!DEBUG_INFO_CONSTANT!>x<!> !== <!DEBUG_INFO_CONSTANT!>y<!>) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableNothing /* = kotlin.Nothing? */")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableNothing /* = kotlin.Nothing? */")!>x<!>.hashCode()
    }
}

// TESTCASE NUMBER: 17
val case_17 = if (nullableIntProperty === <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!>) 0 else {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>nullableIntProperty<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>nullableIntProperty<!>.equals(null)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>nullableIntProperty<!>.propT
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>nullableIntProperty<!>.propAny
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>nullableIntProperty<!>.propNullableT
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>nullableIntProperty<!>.propNullableAny
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>nullableIntProperty<!>.funT()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>nullableIntProperty<!>.funAny()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>nullableIntProperty<!>.funNullableT()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>nullableIntProperty<!>.funNullableAny()
}

//TESTCASE NUMBER: 18
fun case_18(a: DeepObject.A.B.C.D.E.F.G.J?, b: Boolean) {
    val x = null
    val y = null

    if (a != (if (b) <!DEBUG_INFO_CONSTANT!>x<!> else <!DEBUG_INFO_CONSTANT!>y<!>) || <!DEBUG_INFO_CONSTANT!>x<!> !== <!DEBUG_INFO_CONSTANT!>a<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.funNullableAny()
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
                            } else <!DEBUG_INFO_CONSTANT!>z<!>
                        }
                    } else <!DEBUG_INFO_CONSTANT!>z<!>
                }
            } else <!DEBUG_INFO_CONSTANT!>z<!>
        }
    } else <!DEBUG_INFO_CONSTANT!>z<!>

    if (a != <!DEBUG_INFO_CONSTANT!>z<!> && <!DEBUG_INFO_SMARTCAST!>a<!>.B19 !== <!DEBUG_INFO_CONSTANT!>z<!> && <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19 != <!DEBUG_INFO_CONSTANT!>z<!> && <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19 != <!DEBUG_INFO_CONSTANT!>z<!> && <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x !== <!DEBUG_INFO_CONSTANT!>z<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 20
fun case_20(x: Boolean, y: Nothing?) {
    val z = object {
        val B19 = object {
            val C19 = object {
                val D19 =  if (x) {
                    object {}
                } else <!DEBUG_INFO_CONSTANT!>y<!>
            }
        }
    }

    if (z.B19.C19.D19 !== <!DEBUG_INFO_CONSTANT!>y<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided> & case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>?")!>z.B19.C19.D19<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>"), DEBUG_INFO_SMARTCAST!>z.B19.C19.D19<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>"), DEBUG_INFO_SMARTCAST!>z.B19.C19.D19<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>"), DEBUG_INFO_SMARTCAST!>z.B19.C19.D19<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided> & case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>?")!>z.B19.C19.D19<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided> & case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>?")!>z.B19.C19.D19<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>"), DEBUG_INFO_SMARTCAST!>z.B19.C19.D19<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>"), DEBUG_INFO_SMARTCAST!>z.B19.C19.D19<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided> & case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>?")!>z.B19.C19.D19<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided> & case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>?")!>z.B19.C19.D19<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 21
fun case_21() {
    if (EnumClassWithNullableProperty.A.prop_1 !== <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>EnumClassWithNullableProperty.A.prop_1<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>EnumClassWithNullableProperty.A.prop_1<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>EnumClassWithNullableProperty.A.prop_1<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>EnumClassWithNullableProperty.A.prop_1<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>EnumClassWithNullableProperty.A.prop_1<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>EnumClassWithNullableProperty.A.prop_1<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>EnumClassWithNullableProperty.A.prop_1<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>EnumClassWithNullableProperty.A.prop_1<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>EnumClassWithNullableProperty.A.prop_1<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>EnumClassWithNullableProperty.A.prop_1<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 22
fun case_22(a: (() -> Unit)?) {
    if (a != <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>a<!>()<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>a<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>a<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>a<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit")!>a<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 23
fun case_23(a: ((Float) -> Int?)?, b: Float?, z: Nothing?) {
    if (a != <!DEBUG_INFO_CONSTANT!>z<!> && b !== <!DEBUG_INFO_CONSTANT!>z<!> && b !== <!DEBUG_INFO_CONSTANT!>z<!>) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!><!DEBUG_INFO_EXPRESSION_TYPE("((kotlin.Float) -> kotlin.Int?)? & (kotlin.Float) -> kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>)<!>
        if (x != <!DEBUG_INFO_CONSTANT!>z<!> || <!DEBUG_INFO_CONSTANT!>x<!> !== <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!>) {
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
fun case_24(a: ((() -> Unit) -> Unit)?, b: (() -> Unit)?, z: Nothing?) =
    if (a !== <!DEBUG_INFO_CONSTANT!>z<!> && b !== <!DEBUG_INFO_CONSTANT!>z<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("((() -> kotlin.Unit) -> kotlin.Unit)? & (() -> kotlin.Unit) -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>b<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("((() -> kotlin.Unit) -> kotlin.Unit)? & (() -> kotlin.Unit) -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>a<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("((() -> kotlin.Unit) -> kotlin.Unit)? & (() -> kotlin.Unit) -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("((() -> kotlin.Unit) -> kotlin.Unit)? & (() -> kotlin.Unit) -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>a<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("((() -> kotlin.Unit) -> kotlin.Unit)? & (() -> kotlin.Unit) -> kotlin.Unit")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("((() -> kotlin.Unit) -> kotlin.Unit)? & (() -> kotlin.Unit) -> kotlin.Unit")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("((() -> kotlin.Unit) -> kotlin.Unit)? & (() -> kotlin.Unit) -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("((() -> kotlin.Unit) -> kotlin.Unit)? & (() -> kotlin.Unit) -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>a<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("((() -> kotlin.Unit) -> kotlin.Unit)? & (() -> kotlin.Unit) -> kotlin.Unit")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("((() -> kotlin.Unit) -> kotlin.Unit)? & (() -> kotlin.Unit) -> kotlin.Unit")!>a<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>b<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>b<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>b<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit")!>b<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit")!>b<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>b<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>b<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit")!>b<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit")!>b<!>.funNullableAny()
    } else <!DEBUG_INFO_CONSTANT!>z<!>

// TESTCASE NUMBER: 25
fun case_25(b: Boolean, z: Nothing?) {
    val x = {
        if (b) object {
            val a = 10
        } else <!DEBUG_INFO_CONSTANT!>z<!>
    }

    val y = if (b) x else <!DEBUG_INFO_CONSTANT!>z<!>

    if (y !== <!DEBUG_INFO_CONSTANT!>z<!> || <!DEBUG_INFO_CONSTANT!>y<!> != <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!>) {
        val z1 = <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided>?")!><!DEBUG_INFO_EXPRESSION_TYPE("(() -> case_25.<anonymous>.<no name provided>?)? & () -> case_25.<anonymous>.<no name provided>?"), DEBUG_INFO_SMARTCAST!>y<!>()<!>

        if (z1 != <!DEBUG_INFO_CONSTANT!>z<!> && <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> !== z1) {
            <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided> & case_25.<anonymous>.<no name provided>?"), DEBUG_INFO_SMARTCAST!>z1<!>.a
            <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided> & case_25.<anonymous>.<no name provided>?"), DEBUG_INFO_SMARTCAST!>z1<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided> & case_25.<anonymous>.<no name provided>?"), DEBUG_INFO_SMARTCAST!>z1<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided> & case_25.<anonymous>.<no name provided>?"), DEBUG_INFO_SMARTCAST!>z1<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided> & case_25.<anonymous>.<no name provided>?")!>z1<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided> & case_25.<anonymous>.<no name provided>?")!>z1<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided> & case_25.<anonymous>.<no name provided>?"), DEBUG_INFO_SMARTCAST!>z1<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided> & case_25.<anonymous>.<no name provided>?"), DEBUG_INFO_SMARTCAST!>z1<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided> & case_25.<anonymous>.<no name provided>?")!>z1<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided> & case_25.<anonymous>.<no name provided>?")!>z1<!>.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 26
fun case_26(a: ((Float) -> Int?)?, b: Float?) {
    var z = null

    if (a != <!DEBUG_INFO_CONSTANT!>z<!> == true && b != <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> == true) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!><!DEBUG_INFO_EXPRESSION_TYPE("((kotlin.Float) -> kotlin.Int?)? & (kotlin.Float) -> kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>)<!>
        if (x != <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> == true || <!DEBUG_INFO_CONSTANT!>z<!> !== <!DEBUG_INFO_CONSTANT!>x<!>) {
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

// TESTCASE NUMBER: 27
fun case_27(z: Nothing?) {
    if (Object.prop_1 == <!DEBUG_INFO_CONSTANT!>z<!> == true == true == true == true == true == true == true == true == true == true == true == true == true == true)
    else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>Object.prop_1<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!>Object.prop_1<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!>Object.prop_1<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!>Object.prop_1<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>Object.prop_1<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>Object.prop_1<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!>Object.prop_1<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!>Object.prop_1<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>Object.prop_1<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>Object.prop_1<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 28
fun case_28(a: DeepObject.A.B.C.D.E.F.G.J?) =
    if (a != <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> == true == false == false == false == true == false == true == false == false == true == true) {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.x
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.funNullableAny()
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

    if (false || false || false || false || y !== <!DEBUG_INFO_CONSTANT!>v<!>) {
        val t = <!DEBUG_INFO_EXPRESSION_TYPE("case_29.<anonymous>.<no name provided>?")!><!UNSAFE_CALL!>y<!>()<!>

        if (<!EQUALITY_NOT_APPLICABLE!>z !== t<!> || false) {
            <!DEBUG_INFO_EXPRESSION_TYPE("case_29.<anonymous>.<no name provided>?")!>t<!><!UNSAFE_CALL!>.<!>a
            <!DEBUG_INFO_EXPRESSION_TYPE("case_29.<anonymous>.<no name provided>?")!>t<!><!UNSAFE_CALL!>.<!>equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("case_29.<anonymous>.<no name provided>?")!>t<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("case_29.<anonymous>.<no name provided>?")!>t<!><!UNSAFE_CALL!>.<!>propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("case_29.<anonymous>.<no name provided>?")!>t<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("case_29.<anonymous>.<no name provided>?")!>t<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("case_29.<anonymous>.<no name provided>?")!>t<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("case_29.<anonymous>.<no name provided>?")!>t<!><!UNSAFE_CALL!>.<!>funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("case_29.<anonymous>.<no name provided>?")!>t<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("case_29.<anonymous>.<no name provided>?")!>t<!>.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 30
fun case_30(a: ((Float) -> Int?)?, b: Float?) {
    if (<!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> != a == true && b != <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> == true || false || false || false || false || false || false || false || false || false) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!><!DEBUG_INFO_EXPRESSION_TYPE("((kotlin.Float) -> kotlin.Int?)? & (kotlin.Float) -> kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>)<!>
        if (false || <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> != x == true) {
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

// TESTCASE NUMBER: 31
fun case_31(z1: Boolean?, z: Nothing?) {
    if (false || EnumClassWithNullableProperty.A.prop_1 != <!DEBUG_INFO_CONSTANT!>z<!> && z1 !== <!DEBUG_INFO_CONSTANT!>z<!> && <!DEBUG_INFO_SMARTCAST!>z1<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>EnumClassWithNullableProperty.A.prop_1<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>EnumClassWithNullableProperty.A.prop_1<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>EnumClassWithNullableProperty.A.prop_1<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>EnumClassWithNullableProperty.A.prop_1<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>EnumClassWithNullableProperty.A.prop_1<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>EnumClassWithNullableProperty.A.prop_1<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>EnumClassWithNullableProperty.A.prop_1<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>EnumClassWithNullableProperty.A.prop_1<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>EnumClassWithNullableProperty.A.prop_1<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>EnumClassWithNullableProperty.A.prop_1<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 32
fun case_32(a: DeepObject.A.B.C.D.E.F.G.J?) =
    if (a == <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> == true == false == false == false == true == false == true == false == false == true == true && true) {
        -1
    } else {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.x
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.funNullableAny()
    }

// TESTCASE NUMBER: 33
fun case_33(a: ((Float) -> Int?)?, b: Float?, c: Boolean?) {
    var z = null

    if (true && a == <!DEBUG_INFO_CONSTANT!>z<!> == true || b == null == true) {

    } else {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!><!DEBUG_INFO_EXPRESSION_TYPE("((kotlin.Float) -> kotlin.Int?)? & (kotlin.Float) -> kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>)<!>
        if (x == <!DEBUG_INFO_CONSTANT!>z<!> == true && <!DEBUG_INFO_CONSTANT!>x<!> === <!DEBUG_INFO_CONSTANT!>z<!> || (c != <!DEBUG_INFO_CONSTANT!>z<!> && !<!DEBUG_INFO_SMARTCAST!>c<!>)) {

        } else {
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

/*
 * TESTCASE NUMBER: 34
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28329
 */
fun case_34(z1: Boolean?) {
    var z = null

    if (true && true && true && true && EnumClassWithNullableProperty.A.prop_1 != <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> && <!SENSELESS_COMPARISON!>EnumClassWithNullableProperty.A.prop_1 !== null<!> && EnumClassWithNullableProperty.A.prop_1 !== <!DEBUG_INFO_CONSTANT!>z<!> || z1 != <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> || <!ALWAYS_NULL!>z1<!>!! && true && true) {

    } else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>EnumClassWithNullableProperty.A.prop_1<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>EnumClassWithNullableProperty.A.prop_1<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>EnumClassWithNullableProperty.A.prop_1<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>EnumClassWithNullableProperty.A.prop_1<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>EnumClassWithNullableProperty.A.prop_1<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>EnumClassWithNullableProperty.A.prop_1<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>EnumClassWithNullableProperty.A.prop_1<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>EnumClassWithNullableProperty.A.prop_1<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>EnumClassWithNullableProperty.A.prop_1<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>EnumClassWithNullableProperty.A.prop_1<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 35
fun case_35(a: DeepObject.A.B.C.D.E.F.G.J?) {
    val itest = false

    if (true && a != <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> && a !== <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> || itest || !itest || true || !true) {

    } else {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J? & kotlin.Nothing?")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J? & kotlin.Nothing?")!>a<!>.hashCode()
    }
}

// TESTCASE NUMBER: 36
fun case_36(x: Any) {
    var z = null

    if (x == <!DEBUG_INFO_CONSTANT!>z<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Nothing")!>x<!>
    }
}

// TESTCASE NUMBER: 37
fun case_37(x: Nothing?, y: Nothing?) {
    if (<!DEBUG_INFO_CONSTANT!>x<!> == <!DEBUG_INFO_CONSTANT!>y<!>) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>.hashCode()
    }
}

// TESTCASE NUMBER: 38
fun case_38() {
    val z = null

    if (Object.prop_2 != <!DEBUG_INFO_CONSTANT!>z<!>)
    else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing & kotlin.Number")!>Object.prop_2<!>
    }
}

// TESTCASE NUMBER: 39
fun case_39(x: Char?) {
    if (x == <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> && true) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char? & kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char? & kotlin.Nothing?")!>x<!>.hashCode()
    }
}

// TESTCASE NUMBER: 40
fun case_40() {
    val x: Unit? = null
    var z = null

    if (x == <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> || <!DEBUG_INFO_CONSTANT!>z<!> === x) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.Unit?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.Unit?")!>x<!>.hashCode()
    }
}

// TESTCASE NUMBER: 41
fun case_41(x: EmptyClass?, z: Nothing?) {
    val y = true

    if (x === <!DEBUG_INFO_CONSTANT!>z<!> && !y) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("EmptyClass? & kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass? & kotlin.Nothing?")!>x<!>.hashCode()
    }
}

// TESTCASE NUMBER: 42
fun case_42() {
    if (EmptyObject == <!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!> || <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>EmptyObject<!> === <!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject & kotlin.Nothing")!>EmptyObject<!>
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

    if (x == <!DEBUG_INFO_CONSTANT!>z<!> && <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String? */ & kotlin.Nothing?")!>x<!> == <!DEBUG_INFO_CONSTANT!>z<!>) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String? */ & kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String? */ & kotlin.Nothing?")!>x<!>.hashCode()
    }
}

/*
 * TESTCASE NUMBER: 44
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28329
 */
fun case_44(x: TypealiasNullableString<!REDUNDANT_NULLABLE!>?<!>, z1: Nothing?) {
    if (true && true && true && true && x !== <!DEBUG_INFO_CONSTANT!>z1<!>) {

    } else if (false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? /* = kotlin.String? */")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? /* = kotlin.String? */")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? /* = kotlin.String? */")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? /* = kotlin.String? */")!>x<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? /* = kotlin.String? */")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? /* = kotlin.String? */")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? /* = kotlin.String? */")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? /* = kotlin.String? */")!>x<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? /* = kotlin.String? */")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? /* = kotlin.String? */")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 45
fun case_45() {
    val a = Class()
    var z: Nothing? = null

    if (a.prop_4 != <!DEBUG_INFO_CONSTANT!>z<!> || true) {
        if (a.prop_4 == null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float? & kotlin.Nothing?")!>a.prop_4<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float? & kotlin.Nothing?")!>a.prop_4<!>.hashCode()
        }
    }
}

// TESTCASE NUMBER: 46
fun case_46(x: TypealiasNullableString<!REDUNDANT_NULLABLE!>?<!>, y: TypealiasNullableString) {
    val t: TypealiasNullableString = null
    var z: Nothing? = null

    if (x != <!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!>) {

    } else {
        if (y === <!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!>) {
            if (<!DEBUG_INFO_CONSTANT!>z<!> != nullableStringProperty) {
                if (<!DEBUG_INFO_CONSTANT!>z<!> === t || t == <!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!>) {
                    <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? /* = kotlin.String? */ & kotlin.Nothing?")!>x<!>
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? /* = kotlin.String? */ & kotlin.Nothing?")!>x<!>.hashCode()
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
fun case_47(x: TypealiasNullableString, y: TypealiasNullableString, z: Nothing?) = if (x !== <!DEBUG_INFO_CONSTANT!>z<!> && true && true && true) "1"
    else if (y != <!DEBUG_INFO_CONSTANT!>z<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String? */")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String? */")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String? */")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String? */")!>x<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String? */")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String? */")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String? */")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String? */")!>x<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String? */")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String? */")!>x<!>.funNullableAny()
    } else "-1"

// TESTCASE NUMBER: 48
fun case_48(x: EmptyClass12_48?, z: Nothing?) =
    if (x != <!DEBUG_INFO_CONSTANT!>z<!> && true) {
        throw Exception()
    } else {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & othertypes.EmptyClass12_48?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & othertypes.EmptyClass12_48?")!>x<!>.hashCode()
    }

// TESTCASE NUMBER: 49
class Case49 {
    val x: TypealiasNullableString<!REDUNDANT_NULLABLE!>?<!>
    init {
        x = TypealiasNullableString()
    }
}

fun case_49() {
    val a = Case49()
    var z = null

    if (a.x === <!DEBUG_INFO_CONSTANT!>z<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? /* = kotlin.String? */ & kotlin.Nothing?")!>a.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString? /* = kotlin.String? */ & kotlin.Nothing?")!>a.x<!>.hashCode()
    }
}

// TESTCASE NUMBER: 50
fun case_50(x: TypealiasNullableString) {
    val z1 = null
    val z2 = null
    val <!UNUSED_VARIABLE!>t<!> = if (x != <!DEBUG_INFO_CONSTANT!>z1<!> && <!DEBUG_INFO_CONSTANT!>z2<!> !== x) "" else {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String? */ & kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String? */ & kotlin.Nothing?")!>x<!>.hashCode()
    }
}

// TESTCASE NUMBER: 51
fun case_51() {
    val x: TypealiasNullableNothing = null
    val z: Nothing? = null

    if (<!DEBUG_INFO_CONSTANT!>x<!> === <!DEBUG_INFO_CONSTANT!>z<!> || <!DEBUG_INFO_CONSTANT!>z<!> == <!DEBUG_INFO_CONSTANT!>x<!> && <!DEBUG_INFO_CONSTANT!>x<!> == <!DEBUG_INFO_CONSTANT!>z<!> || false || false || false) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableNothing /* = kotlin.Nothing? */")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableNothing /* = kotlin.Nothing? */")!>x<!>.hashCode()
    }
}

// TESTCASE NUMBER: 52
val case_52 = if (nullableIntProperty !== <!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!> && <!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!> != nullableIntProperty) 0 else {
    <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Nothing?")!>nullableIntProperty<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Nothing?")!>nullableIntProperty<!>.hashCode()
}

//TESTCASE NUMBER: 53
fun case_53(a: DeepObject.A.B.C.D.E.F.G.J?) {
    if (a == DeepObject.prop_2) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J? & kotlin.Nothing?")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J? & kotlin.Nothing?")!>a<!>.hashCode()
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

    if (a != <!DEBUG_INFO_CONSTANT!>z<!> && <!DEBUG_INFO_SMARTCAST!>a<!>.B54 !== <!DEBUG_INFO_SMARTCAST!>a<!>.z && <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B54<!>.C54 != <!DEBUG_INFO_SMARTCAST!>a<!>.z && <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B54<!>.C54<!>.D54 != <!DEBUG_INFO_SMARTCAST!>a<!>.z && <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B54<!>.C54<!>.D54<!>.x === <!DEBUG_INFO_SMARTCAST!>a<!>.z) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.Number?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B54<!>.C54<!>.D54<!>.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.Number?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B54<!>.C54<!>.D54<!>.x<!>.hashCode()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("case_55.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>? & kotlin.Nothing?")!>a.B19.C19.D19<!>
    }
}

// TESTCASE NUMBER: 56
fun case_56() {
    if (EnumClassWithNullableProperty.A.prop_1 == <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Nothing?")!>EnumClassWithNullableProperty.A.prop_1<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Nothing?")!>EnumClassWithNullableProperty.A.prop_1<!>.hashCode()
    }
}

// TESTCASE NUMBER: 57
fun case_57(a: (() -> Unit)) {
    var z = null

    if (a == <!DEBUG_INFO_CONSTANT!>z<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("() -> kotlin.Unit & kotlin.Nothing")!>a<!>
    }
}

// TESTCASE NUMBER: 58
fun case_58(a: ((Float) -> Int?)?, b: Float?, z: Nothing?) {
    if (a === <!DEBUG_INFO_CONSTANT!>z<!> && b == <!DEBUG_INFO_CONSTANT!>z<!> || <!DEBUG_INFO_CONSTANT!>z<!> == a && <!DEBUG_INFO_CONSTANT!>z<!> === b) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("((kotlin.Float) -> kotlin.Int?)? & kotlin.Nothing?")!>a<!>
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float? & kotlin.Nothing?")!>b<!>
        if (<!DEBUG_INFO_CONSTANT!>a<!> != <!DEBUG_INFO_CONSTANT!>z<!>) {
            <!DEBUG_INFO_EXPRESSION_TYPE("((kotlin.Float) -> kotlin.Int?)? & (kotlin.Float) -> kotlin.Int? & kotlin.Nothing")!>a<!>
        }
    }
}

/*
 * TESTCASE NUMBER: 59
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28329
 */
fun case_59(a: ((() -> Unit) -> Unit)?, b: (() -> Unit)?, z: Nothing?) {
    if (false || false || a == <!DEBUG_INFO_CONSTANT!>z<!> && b === <!DEBUG_INFO_CONSTANT!>z<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("((() -> kotlin.Unit) -> kotlin.Unit)?")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)?")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("((() -> kotlin.Unit) -> kotlin.Unit)?")!>a<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("((() -> kotlin.Unit) -> kotlin.Unit)?")!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("((() -> kotlin.Unit) -> kotlin.Unit)?")!>a<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("((() -> kotlin.Unit) -> kotlin.Unit)?")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("((() -> kotlin.Unit) -> kotlin.Unit)?")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("((() -> kotlin.Unit) -> kotlin.Unit)?")!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("((() -> kotlin.Unit) -> kotlin.Unit)?")!>a<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("((() -> kotlin.Unit) -> kotlin.Unit)?")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("((() -> kotlin.Unit) -> kotlin.Unit)?")!>a<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)?")!>b<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)?")!>b<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)?")!>b<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)?")!>b<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)?")!>b<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)?")!>b<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)?")!>b<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)?")!>b<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)?")!>b<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 60
fun case_60(b: Boolean) {
    val x = {
        if (b) object {
            val a = 10
        } else <!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!>
    }

    val y = if (b) x else <!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!>

    if (y != <!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!>) {
        val z = <!DEBUG_INFO_EXPRESSION_TYPE("case_60.<anonymous>.<no name provided>?")!><!DEBUG_INFO_EXPRESSION_TYPE("(() -> case_60.<anonymous>.<no name provided>?)? & () -> case_60.<anonymous>.<no name provided>?"), DEBUG_INFO_SMARTCAST!>y<!>()<!>

        if (z == <!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!>) {
            <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("case_60.<anonymous>.<no name provided>? & kotlin.Nothing?")!>z<!>
        }
    }
}

// TESTCASE NUMBER: 61
fun case_61(x: Any?) {
    if (x is Number?) {
        if (x !== <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!>) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Number")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number"), DEBUG_INFO_SMARTCAST!>x<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number"), DEBUG_INFO_SMARTCAST!>x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Number")!>x<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number"), DEBUG_INFO_SMARTCAST!>x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Number")!>x<!>.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 62
fun case_62(x: Any?) {
    var z = null
    if (x is Number? && x is Int? && x != <!DEBUG_INFO_CONSTANT!>z<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Int & kotlin.Number")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int"), DEBUG_INFO_SMARTCAST!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Int & kotlin.Number")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int"), DEBUG_INFO_SMARTCAST!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Int & kotlin.Number")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 63
fun case_63(x: Any?, b: Boolean) {
    val z1 = null
    val z2 = null
    val z3 = null

    if (x is Number?) {
        if (x !== when (b) { true -> <!DEBUG_INFO_CONSTANT!>z1<!>; false -> <!DEBUG_INFO_CONSTANT!>z2<!>; <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> <!DEBUG_INFO_CONSTANT!>z3<!> }) {
            if (x is Int?) {
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Int & kotlin.Number")!>x<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int"), DEBUG_INFO_SMARTCAST!>x<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int"), DEBUG_INFO_SMARTCAST!>x<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Int & kotlin.Number")!>x<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int"), DEBUG_INFO_SMARTCAST!>x<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Int & kotlin.Number")!>x<!>.funNullableAny()
            }
        }
    }
}

// TESTCASE NUMBER: 64
fun case_64(x: Any?) {
    if (x != try {<!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!>} finally {}) {
        if (x is Number) {
            if (x is Int?) {
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Int & kotlin.Number")!>x<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int"), DEBUG_INFO_SMARTCAST!>x<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int"), DEBUG_INFO_SMARTCAST!>x<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Int & kotlin.Number")!>x<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int"), DEBUG_INFO_SMARTCAST!>x<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Int & kotlin.Number")!>x<!>.funNullableAny()
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
                        if (x != <!DEBUG_INFO_CONSTANT!>z<!>) {
                            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & ClassLevel2 & ClassLevel3 & ClassLevel4 & ClassLevel5 & kotlin.Any & kotlin.Any?")!>x<!>
                            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
                            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
                            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
                            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.propNullableT
                            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & ClassLevel2 & ClassLevel3 & ClassLevel4 & ClassLevel5 & kotlin.Any & kotlin.Any?")!>x<!>.propNullableAny
                            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
                            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
                            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.funNullableT()
                            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & ClassLevel2 & ClassLevel3 & ClassLevel4 & ClassLevel5 & kotlin.Any & kotlin.Any?")!>x<!>.funNullableAny()
                        }
                    }
                }
            }
        }
    }
}

// TESTCASE NUMBER: 66
fun case_66(x: Any?, z1: Nothing?, z2: Nothing?, b: Boolean) {
    if (x is ClassLevel1?) {
        if (x is ClassLevel2?) {
            if (x is ClassLevel3?) {
                if (x != if (b) { <!DEBUG_INFO_CONSTANT!>z1<!> } else { <!DEBUG_INFO_CONSTANT!>z2<!> } && x is ClassLevel4?) {
                    if (x is ClassLevel5?) {
                        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & ClassLevel2 & ClassLevel3 & ClassLevel4 & ClassLevel5 & kotlin.Any & kotlin.Any?")!>x<!>
                        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
                        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
                        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
                        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.propNullableT
                        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & ClassLevel2 & ClassLevel3 & ClassLevel4 & ClassLevel5 & kotlin.Any & kotlin.Any?")!>x<!>.propNullableAny
                        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
                        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
                        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.funNullableT()
                        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & ClassLevel2 & ClassLevel3 & ClassLevel4 & ClassLevel5 & kotlin.Any & kotlin.Any?")!>x<!>.funNullableAny()
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
        if (x is ClassLevel4? && x != (fun (): Nothing? { return <!DEBUG_INFO_CONSTANT!>z<!> })() && x is ClassLevel5?) {
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & ClassLevel2 & ClassLevel3 & ClassLevel4 & ClassLevel5 & kotlin.Any & kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & ClassLevel2 & ClassLevel3 & ClassLevel4 & ClassLevel5 & kotlin.Any & kotlin.Any?")!>x<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & ClassLevel2 & ClassLevel3 & ClassLevel4 & ClassLevel5 & kotlin.Any & kotlin.Any?")!>x<!>.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 68
fun case_68(x: Any?, z: Nothing?) {
    if (x is ClassLevel1? && x is ClassLevel2? && x is ClassLevel3?) {
        if (x is ClassLevel4? && x != (fun (): Nothing? { return <!DEBUG_INFO_CONSTANT!>z<!> })() && x is ClassLevel5?) {
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & ClassLevel2 & ClassLevel3 & ClassLevel4 & ClassLevel5 & kotlin.Any & kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & ClassLevel2 & ClassLevel3 & ClassLevel4 & ClassLevel5 & kotlin.Any & kotlin.Any?")!>x<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & ClassLevel2 & ClassLevel3 & ClassLevel4 & ClassLevel5 & kotlin.Any & kotlin.Any?")!>x<!>.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 69
fun case_69(x: Any?, z: Nothing?) {
    if (x is ClassLevel1? && x is ClassLevel2? && x is ClassLevel3? && x is ClassLevel4? && x != try { <!DEBUG_INFO_CONSTANT!>z<!> } catch (e: Exception) { <!DEBUG_INFO_CONSTANT!>z<!> } && x is ClassLevel5?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & ClassLevel2 & ClassLevel3 & ClassLevel4 & ClassLevel5 & kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & ClassLevel2 & ClassLevel3 & ClassLevel4 & ClassLevel5 & kotlin.Any & kotlin.Any?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & ClassLevel2 & ClassLevel3 & ClassLevel4 & ClassLevel5 & kotlin.Any & kotlin.Any?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 70
fun case_70(x: Any?) {
    if (x is ClassLevel1? && x is ClassLevel2? && x is ClassLevel3?) {
        if (x is ClassLevel4?) {

        } else if (x is ClassLevel5? && x != <!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!> || x != <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!>) {
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & ClassLevel2 & ClassLevel3 & kotlin.Any & kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel3 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel3 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & ClassLevel2 & ClassLevel3 & kotlin.Any & kotlin.Any?")!>x<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel3 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel3 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & ClassLevel2 & ClassLevel3 & kotlin.Any & kotlin.Any?")!>x<!>.funNullableAny()
        }
    } else if (x is ClassLevel4? && x !== <!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!> && x is ClassLevel5?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel4 & ClassLevel5 & kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel4 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel4 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel4 & ClassLevel5 & kotlin.Any & kotlin.Any?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel4 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel4 & ClassLevel5 & kotlin.Any & kotlin.Any?")!>x<!>.funNullableAny()
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
    var z2 = <!DEBUG_INFO_CONSTANT!>z1<!>

    if (t is Interface1?) {
        if (t is Interface2?) {
            if (t != <!DEBUG_INFO_CONSTANT!>z2<!>) {
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & kotlin.Any & kotlin.Any?")!>t<!>
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>.itest1()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>.itest2()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>.itest()

                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>.let { <!DEBUG_INFO_EXPRESSION_TYPE("{Any & Interface1 & Interface2}")!>it<!>.itest1(); <!DEBUG_INFO_EXPRESSION_TYPE("{Any & Interface1 & Interface2}")!>it<!>.itest2() }
            }
        }
    }
}

/*
 * TESTCASE NUMBER: 72
 * NOTE: lazy smartcasts
 * DISCUSSION
 * ISSUES: KT-28362, KT-27032
 */
fun case_72(t: Any?, z1: Nothing?) {
    var z2 = null

    if (t is Interface1? && t != <!DEBUG_INFO_CONSTANT!>z1<!> ?: <!DEBUG_INFO_CONSTANT!>z2<!> && t is Interface2?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & Interface2 & kotlin.Any & kotlin.Any?")!>t<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>.itest1()
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>.itest2()
        <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>.itest()

        <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>.let { <!DEBUG_INFO_EXPRESSION_TYPE("{Any & Interface1 & Interface2}")!>it<!>.itest1(); <!DEBUG_INFO_EXPRESSION_TYPE("{Any & Interface1 & Interface2}")!>it<!>.itest2() }
    }
}

/*
 * TESTCASE NUMBER: 73
 * NOTE: lazy smartcasts
 * DISCUSSION
 * ISSUES: KT-28362
 */
fun case_73(t: Any?) {
    val `null` = null

    if (t is Interface2?) {
        if (t is ClassLevel1?) {
            if (t is ClassLevel2? && t is Interface1?) {
                if (t !is Interface3?) {} else if (false) {
                    if (t != <!DEBUG_INFO_CONSTANT!>`null`<!>) {
                        <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>.itest2()
                        <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>.itest1()
                        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>.itest()
                        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>.test1()
                        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel2 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>.test2()
                        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & ClassLevel2 & Interface1 & Interface2 & Interface3 & kotlin.Any & kotlin.Any?")!>t<!>
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
            if (t == <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> || t === <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> || t !is Interface1?) else {
                if (t is ClassLevel2?) {
                    if (t is Interface3?) {
                        <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>.itest2()
                        <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>.itest1()
                        <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>.itest()
                        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>.test1()
                        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel2 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>.test2()
                        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & ClassLevel2 & Interface1 & Interface2 & Interface3 & kotlin.Any & kotlin.Any?")!>t<!>
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
    if (t !is ClassLevel2? || <!USELESS_IS_CHECK!>t !is ClassLevel1?<!>) else {
        if (t === ((((((<!DEBUG_INFO_CONSTANT!>z<!>)))))) || t !is Interface1?) else {
            if (t !is Interface2? || t !is Interface3?) {} else {
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>.itest2()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface1 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>.itest1()
                <!DEBUG_INFO_EXPRESSION_TYPE("Interface3 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>.itest()
                <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel2 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>.test1()
                <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel2 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>.test2()
                <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel2 & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>t<!>
            }
        }
    }
}

// TESTCASE NUMBER: 76
fun case_76(a: Any?, b: Int = if (<!DEPRECATED_IDENTITY_EQUALS!>a !is Number? === true<!> || a !is Int? == true || a != null == false == true) 0 else <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int"), DEBUG_INFO_SMARTCAST!>a<!>) {
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
