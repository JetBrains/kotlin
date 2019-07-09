// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 7
 * DESCRIPTION: Raw data flow analysis test
 * UNEXPECTED BEHAVIOUR
 * HELPERS: classes, enumClasses, objects, typealiases, properties, functions
 */

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
    if (x != null || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!> || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!> || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!> || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!> || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!> || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!>) {
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
    if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> !== null<!> && <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> !== null<!>) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
    }
}

// TESTCASE NUMBER: 3
fun case_3() {
    if (Object.prop_1 == null && <!SENSELESS_COMPARISON!>Object.prop_1 == null<!>)
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
fun case_4(x: Char?) {
    if (x != null && true || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!>) {
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

    if (x !== null || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> !== null<!> && <!SENSELESS_COMPARISON!>x !== null<!> || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> !== null<!> && <!SENSELESS_COMPARISON!>x !== null<!>) {
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
fun case_6(x: Class?) {
    val y = true

    if (((false || x != null || false) && !y) || x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 7
fun case_7() {
    val x: EmptyObject? = null
    if (x != null || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!> || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("EmptyObject? & kotlin.Nothing?")!>x<!> != null<!>) {
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
fun case_8(x: TypealiasString) {
    if (<!SENSELESS_COMPARISON!>x !== null<!> && <!SENSELESS_COMPARISON!><!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */")!>x<!> != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 9
fun case_9(x: TypealiasNullableString<!REDUNDANT_NULLABLE!>?<!>) {
    if (x === null && <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> === null<!> || <!SENSELESS_COMPARISON!>x === null<!>) {

    } else if (<!SENSELESS_COMPARISON!>x === null<!> || <!SENSELESS_COMPARISON!>x === null<!>) {
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

    if (a.prop_4 === null || <!SENSELESS_COMPARISON!>a.prop_4 === null<!> || true) {
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
    val t: TypealiasNullableString = null

    if (x == null) {

    } else {
        if (<!SENSELESS_COMPARISON!>x != null<!>) {
            if (y != null || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>y<!> != null<!>) {
                if (<!SENSELESS_COMPARISON!>stringProperty == null<!> && <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!> == null<!>) {
                    if (t != null || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>t<!> != null<!>) {
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
}

// TESTCASE NUMBER: 12
fun case_12(x: TypealiasNullableString, y: TypealiasNullableString) = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>if (x == null) "1"
    else if (y === null || <!SENSELESS_COMPARISON!>y === null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>
    } else "-1"<!>

// TESTCASE NUMBER: 13
fun case_13(x: orherpackage.EmptyClass13?, y: Nothing?) =
    <!DEBUG_INFO_EXPRESSION_TYPE("orherpackage.EmptyClass13")!>if (x == null || x === <!DEBUG_INFO_CONSTANT!>y<!>) {
        throw Exception()
    } else {
        <!DEBUG_INFO_EXPRESSION_TYPE("orherpackage.EmptyClass13 & orherpackage.EmptyClass13?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("orherpackage.EmptyClass13 & orherpackage.EmptyClass13?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("orherpackage.EmptyClass13 & orherpackage.EmptyClass13?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("orherpackage.EmptyClass13 & orherpackage.EmptyClass13?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("orherpackage.EmptyClass13 & orherpackage.EmptyClass13?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("orherpackage.EmptyClass13 & orherpackage.EmptyClass13?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("orherpackage.EmptyClass13 & orherpackage.EmptyClass13?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("orherpackage.EmptyClass13 & orherpackage.EmptyClass13?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("orherpackage.EmptyClass13 & orherpackage.EmptyClass13?")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("orherpackage.EmptyClass13 & orherpackage.EmptyClass13?"), DEBUG_INFO_SMARTCAST!>x<!>
    }<!>

// TESTCASE NUMBER: 14
fun case_14() {
    val a = Class()
    if (a.prop_6 != a.prop_7) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>a.prop_6<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>a.prop_6<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>a.prop_6<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>a.prop_6<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>a.prop_6<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>a.prop_6<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>a.prop_6<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>a.prop_6<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>a.prop_6<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>a.prop_6<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 15
fun case_15(x: TypealiasString?) {
    var y = null
    val <!UNUSED_VARIABLE!>t<!> = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>if (x === null || x == <!DEBUG_INFO_CONSTANT!>y<!> && x === <!DEBUG_INFO_CONSTANT!>y<!>) "" else {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */ & TypealiasString? /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */ & TypealiasString? /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */ & TypealiasString? /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */ & TypealiasString? /* = kotlin.String? */")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */ & TypealiasString? /* = kotlin.String? */")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */ & TypealiasString? /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */ & TypealiasString? /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */ & TypealiasString? /* = kotlin.String? */")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */ & TypealiasString? /* = kotlin.String? */")!>x<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */ & TypealiasString? /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>
    }<!>
}

// TESTCASE NUMBER: 16
fun case_16() {
    val x: TypealiasNothing? = null
    val y: Nothing? = null

    if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!> || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> !== null<!> || <!DEBUG_INFO_CONSTANT!>x<!> != <!DEBUG_INFO_CONSTANT!>y<!>) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("TypealiasNothing? /* = kotlin.Nothing? */")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNothing? /* = kotlin.Nothing? */")!>x<!>.hashCode()
    }
}

// TESTCASE NUMBER: 17
val case_17 = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>if (nullableIntProperty == null || <!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!> === nullableIntProperty) 0 else {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>nullableIntProperty<!>.equals(null)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>nullableIntProperty<!>.propT
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>nullableIntProperty<!>.propAny
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>nullableIntProperty<!>.propNullableT
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>nullableIntProperty<!>.propNullableAny
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>nullableIntProperty<!>.funT()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>nullableIntProperty<!>.funAny()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>nullableIntProperty<!>.funNullableT()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>nullableIntProperty<!>.funNullableAny()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>nullableIntProperty<!>
}<!>

/*
 * TESTCASE NUMBER: 18
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28328
 */
fun case_18(a: DeepObject.A.B.C.D.E.F.G.J?, b: Nothing?) {
    if (a != null || <!DEBUG_INFO_CONSTANT!>b<!> !== <!DEBUG_INFO_CONSTANT!>a<!> || false) {
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

    if (a != null && <!DEBUG_INFO_SMARTCAST!>a<!>.B19 != <!DEBUG_INFO_SMARTCAST!>a<!>.y && <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19 != <!DEBUG_INFO_SMARTCAST!>a<!>.y && <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19 != <!DEBUG_INFO_SMARTCAST!>a<!>.y && <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x != <!DEBUG_INFO_SMARTCAST!>a<!>.y) {
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

    if (a.B19.C19.D19 !== null || a.y != a.B19.C19.D19) {
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided> & case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>?")!>a.B19.C19.D19<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>"), DEBUG_INFO_SMARTCAST!>a.B19.C19.D19<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>"), DEBUG_INFO_SMARTCAST!>a.B19.C19.D19<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>"), DEBUG_INFO_SMARTCAST!>a.B19.C19.D19<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided> & case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>?")!>a.B19.C19.D19<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided> & case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>?")!>a.B19.C19.D19<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>"), DEBUG_INFO_SMARTCAST!>a.B19.C19.D19<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>"), DEBUG_INFO_SMARTCAST!>a.B19.C19.D19<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided> & case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>?")!>a.B19.C19.D19<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided> & case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>?")!>a.B19.C19.D19<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 21
fun case_21() {
    val y = null
    if (EnumClassWithNullableProperty.A.prop_1 !== null && <!DEBUG_INFO_CONSTANT!>y<!> != EnumClassWithNullableProperty.A.prop_1) {
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
    var y = null
    if (a != null || <!DEBUG_INFO_CONSTANT!>y<!> != <!DEBUG_INFO_CONSTANT!>a<!>) {
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
fun case_23(a: ((Float) -> Int?)?, b: Float?, c: Nothing?) {
    if (a != null && b !== null || a != <!DEBUG_INFO_CONSTANT!>c<!> && b !== <!DEBUG_INFO_CONSTANT!>c<!>) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!><!DEBUG_INFO_EXPRESSION_TYPE("((kotlin.Float) -> kotlin.Int?)? & (kotlin.Float) -> kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>)<!>
        if (x != null || <!DEBUG_INFO_CONSTANT!>c<!> !== <!DEBUG_INFO_CONSTANT!>x<!>) {
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
    if (a !== null && b !== null || <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> != a && <!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!> !== b) {
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

    if (y !== null || x()!!.b != <!DEBUG_INFO_CONSTANT!>y<!>) {
        if (x()!!.b != y) {
            val z = <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided>?")!><!DEBUG_INFO_EXPRESSION_TYPE("(() -> case_25.<anonymous>.<no name provided>?)? & () -> case_25.<anonymous>.<no name provided>?"), DEBUG_INFO_SMARTCAST!>y<!>()<!>

            if (z != null) {
                <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided> & case_25.<anonymous>.<no name provided>?"), DEBUG_INFO_SMARTCAST!>z<!>.a
                <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided> & case_25.<anonymous>.<no name provided>?"), DEBUG_INFO_SMARTCAST!>z<!>.equals(null)
                <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided> & case_25.<anonymous>.<no name provided>?"), DEBUG_INFO_SMARTCAST!>z<!>.propT
                <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided> & case_25.<anonymous>.<no name provided>?"), DEBUG_INFO_SMARTCAST!>z<!>.propAny
                <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided> & case_25.<anonymous>.<no name provided>?")!>z<!>.propNullableT
                <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided> & case_25.<anonymous>.<no name provided>?")!>z<!>.propNullableAny
                <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided> & case_25.<anonymous>.<no name provided>?"), DEBUG_INFO_SMARTCAST!>z<!>.funT()
                <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided> & case_25.<anonymous>.<no name provided>?"), DEBUG_INFO_SMARTCAST!>z<!>.funAny()
                <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided> & case_25.<anonymous>.<no name provided>?")!>z<!>.funNullableT()
                <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided> & case_25.<anonymous>.<no name provided>?")!>z<!>.funNullableAny()
            }
        }
    }
}

// TESTCASE NUMBER: 26
fun case_26(a: ((Float) -> Int?)?, b: Float?) {
    var c: Nothing? = null

    if (a != null == true && b != null == true || <!DEBUG_INFO_CONSTANT!>c<!> != a == true && b != <!DEBUG_INFO_CONSTANT!>c<!> == true) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!><!DEBUG_INFO_EXPRESSION_TYPE("((kotlin.Float) -> kotlin.Int?)? & (kotlin.Float) -> kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>)<!>
        if (x != null == true) {
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
fun case_27(y: Nothing?) {
    if (Object.prop_1 == null == true == true == true == true == true == true == true == true == true == true == true == true == true == true || <!DEBUG_INFO_CONSTANT!>y<!> == Object.prop_1 == true == true == true == false == false)
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

//TESTCASE NUMBER: 28
fun case_28(a: DeepObject.A.B.C.D.E.F.G.J?) =
    if (a != null == true == false == false == false == true == false == true == false == false == true == true && <!DEBUG_INFO_SMARTCAST!>a<!>.x !== <!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!>) {
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

// TESTCASE NUMBER: 29
fun case_29(a: Int?, b: Nothing?, <!UNUSED_PARAMETER!>c<!>: Int = if (a === <!DEBUG_INFO_CONSTANT!>b<!>) 0 else <!DEBUG_INFO_SMARTCAST!>a<!>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a<!>
    <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>b<!>
}

// TESTCASE NUMBER: 30
fun case_30(a: Int?, b: Nothing?, <!UNUSED_PARAMETER!>c<!>: Int = if (a === <!DEBUG_INFO_CONSTANT!>b<!> || <!SENSELESS_COMPARISON!>a == null<!>) 0 else <!DEBUG_INFO_SMARTCAST!>a<!>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a<!>
    <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>b<!>
}
