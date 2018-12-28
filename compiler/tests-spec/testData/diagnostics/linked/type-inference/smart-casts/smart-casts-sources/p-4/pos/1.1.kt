// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: type-inference, smart-casts, smart-casts-sources -> paragraph 4 -> sentence 1
 * RELEVANT PLACES:
 *      paragraph 1 -> sentence 2
 *      paragraph 6 -> sentence 1
 *      paragraph 9 -> sentence 3
 *      paragraph 9 -> sentence 4
 * NUMBER: 1
 * DESCRIPTION: Smartcasts from nullability condition (value or reference equality) using if expression and simple types.
 * HELPERS: classes, objects, typealiases, properties, enumClasses
 */

// FILE: other_package.kt

package otherpackage

// TESTCASE NUMBER: 13
class Case13 {}

// TESTCASE NUMBER: 14
typealias Case14 = String?

// FILE: main.kt

import otherpackage.*

// TESTCASE NUMBER: 1
fun case_1(x: Any?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28159
 */
fun case_2(x: Nothing?) {
    if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> !== null<!>) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>.equals(<!DEBUG_INFO_CONSTANT!>x<!>)
    }
}

// TESTCASE NUMBER: 3
fun case_3() {
    if (Object.prop_1 == null)
        else {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>Object.prop_1<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!>Object.prop_1<!>.equals(Object.prop_1)
        }
}

// TESTCASE NUMBER: 4
fun case_4(x: Char?) {
    if (x != null && true) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 5
fun case_5() {
    val x: Unit? = null

    if (x !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>x<!>
    if (x !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
}

// TESTCASE NUMBER: 6
fun case_6(x: EmptyClass?) {
    val y = true

    if (x != null && !y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass & EmptyClass?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass & EmptyClass?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 7
fun case_7() {
    if (nullableNumberProperty != null || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.Number?")!>nullableNumberProperty<!> != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>nullableNumberProperty<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>nullableNumberProperty<!>.equals(nullableNumberProperty)
    }
}

// TESTCASE NUMBER: 8
fun case_8(x: TypealiasNullableString) {
    if (x !== null && <!SENSELESS_COMPARISON!><!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */")!>x<!> != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */")!>x<!>
    if (x !== null && <!SENSELESS_COMPARISON!><!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */")!>x<!> != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
}

// TESTCASE NUMBER: 9
fun case_9(x: TypealiasNullableString<!REDUNDANT_NULLABLE!>?<!>) {
    if (x === null) {

    } else if (false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableString /* = kotlin.String */ & TypealiasNullableString? /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 10
fun case_10() {
    val a = Class()

    if (a.prop_4 === null || true) {
        if (a.prop_4 != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>a.prop_4<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float"), DEBUG_INFO_SMARTCAST!>a.prop_4<!>.equals(a.prop_4)
        }
    }
}

// TESTCASE NUMBER: 11
fun case_11(x: TypealiasNullableStringIndirect<!REDUNDANT_NULLABLE!>?<!>, y: TypealiasNullableStringIndirect) {
    val t: TypealiasNullableStringIndirect = null

    if (x == null) {

    } else {
        if (y != null) {
            if (nullableStringProperty == null) {
                if (t != null) {
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect /* = kotlin.String */ & TypealiasNullableStringIndirect? /* = kotlin.String? */")!>x<!>
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect /* = kotlin.String */ & TypealiasNullableStringIndirect? /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
                }
            }
        }
    }
}

// TESTCASE NUMBER: 12
fun case_12(x: TypealiasNullableStringIndirect, y: TypealiasNullableStringIndirect) =
<!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Boolean & String}> & java.io.Serializable}")!>if (x == null) "1"
else if (y === null) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect /* = kotlin.String */ & TypealiasNullableStringIndirect /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>
else if (<!SENSELESS_COMPARISON!>y === null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableStringIndirect /* = kotlin.String */ & TypealiasNullableStringIndirect /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
else "-1"<!>

// TESTCASE NUMBER: 13
fun case_13(x: otherpackage.Case13?) =
<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean")!>if (x == null) {
    throw Exception()
} else {
    <!DEBUG_INFO_EXPRESSION_TYPE("otherpackage.Case13 & otherpackage.Case13?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("otherpackage.Case13 & otherpackage.Case13?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
}<!>

// TESTCASE NUMBER: 14
class Case14 {
    val x: otherpackage.Case14<!REDUNDANT_NULLABLE!>?<!>
    init {
        x = otherpackage.Case14()
    }
}

fun case_14() {
    val a = Case14()

    if (a.x != null) {
        if (<!SENSELESS_COMPARISON!>a.x != null<!>) {
            if (<!SENSELESS_COMPARISON!>a.x !== null<!>) {
            if (<!SENSELESS_COMPARISON!>a.x != null<!>) {
            if (<!SENSELESS_COMPARISON!>a.x != null<!>) {
            if (<!SENSELESS_COMPARISON!>a.x != null<!>) {
            if (<!SENSELESS_COMPARISON!>a.x !== null<!>) {
            if (<!SENSELESS_COMPARISON!>a.x != null<!>) {
            if (<!SENSELESS_COMPARISON!>a.x != null<!>) {
            if (<!SENSELESS_COMPARISON!>a.x !== null<!>) {
            if (<!SENSELESS_COMPARISON!>a.x != null<!>) {
            if (<!SENSELESS_COMPARISON!>a.x != null<!>) {
            if (<!SENSELESS_COMPARISON!>a.x != null<!>) {
            if (<!SENSELESS_COMPARISON!>a.x !== null<!>) {
            if (<!SENSELESS_COMPARISON!>a.x != null<!>) {
            if (<!SENSELESS_COMPARISON!>a.x !== null<!>) {
            <!DEBUG_INFO_EXPRESSION_TYPE("otherpackage.Case14 /* = kotlin.String */ & otherpackage.Case14? /* = kotlin.String? */")!>a.x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("otherpackage.Case14 /* = kotlin.String */"), DEBUG_INFO_SMARTCAST!>a.x<!>.equals(a.x)
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
    val <!UNUSED_VARIABLE!>t<!> = <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Boolean & String}> & java.io.Serializable}")!>if (<!SENSELESS_COMPARISON!>x === null<!>) "" else {
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyObject")!>x<!>.equals(x)
    }<!>
}

// TESTCASE NUMBER: 16
fun case_16() {
    val x: TypealiasNullableNothing = null

    if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!>) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableNothing /* = kotlin.Nothing? */")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNullableNothing /* = kotlin.Nothing? */")!>x<!>.equals(<!DEBUG_INFO_CONSTANT!>x<!>)
    }
}

// TESTCASE NUMBER: 17
val case_17 = <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Boolean & Byte & Int & Long & Short}> & java.io.Serializable}")!>if (nullableIntProperty == null) 0 else {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>nullableIntProperty<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>nullableIntProperty<!>.equals(nullableIntProperty)
}<!>

//TESTCASE NUMBER: 18
fun case_18(a: DeepObject.A.B.C.D.E.F.G.J?) {
    if (a != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.equals(a)
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

    if (a != null && <!DEBUG_INFO_SMARTCAST!>a<!>.B19 != null && <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19 != null && <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19 != null && <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x<!>.equals(null)
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

    if (a.B19.C19.D19 !== null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided> & case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>?")!>a.B19.C19.D19<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>"), DEBUG_INFO_SMARTCAST!>a.B19.C19.D19<!>.equals(a.B19.C19.D19)
    }
}

// TESTCASE NUMBER: 21
fun case_21() {
    if (EnumClassWithNullableProperty.B.prop_1 !== null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>EnumClassWithNullableProperty.B.prop_1<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>EnumClassWithNullableProperty.B.prop_1<!>.equals(EnumClassWithNullableProperty.B.prop_1)
    }
}

// TESTCASE NUMBER: 22
fun case_22(a: (() -> Unit)?) {
    if (a != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>a<!>()<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>a<!>()<!>.equals(a)
    }
}

// TESTCASE NUMBER: 23
fun case_23(a: ((Float) -> Int?)?, b: Float?) {
    if (a != null && b !== null) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!><!DEBUG_INFO_EXPRESSION_TYPE("((kotlin.Float) -> kotlin.Int?)? & (kotlin.Float) -> kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>)<!>
        if (x != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        }
    }
}

// TESTCASE NUMBER: 24
fun case_24(a: ((() -> Unit) -> Unit)?, b: (() -> Unit)?) =
    if (a !== null && b !== null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("((() -> kotlin.Unit) -> kotlin.Unit)? & (() -> kotlin.Unit) -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>b<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("((() -> kotlin.Unit) -> kotlin.Unit)? & (() -> kotlin.Unit) -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>a<!>(<!DEBUG_INFO_SMARTCAST!>b<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit)? & () -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>b<!>.equals(null)
    } else null

// TESTCASE NUMBER: 25
fun case_25(b: Boolean) {
    val x = {
        if (b) object {
            val a = 10
        } else null
    }

    val y = if (b) x else null

    if (y !== null) {
        val z = <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided>?")!><!DEBUG_INFO_EXPRESSION_TYPE("(() -> case_25.<anonymous>.<no name provided>?)? & () -> case_25.<anonymous>.<no name provided>?"), DEBUG_INFO_SMARTCAST!>y<!>()<!>

        if (z != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided> & case_25.<anonymous>.<no name provided>?"), DEBUG_INFO_SMARTCAST!>z<!>.a
            <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided> & case_25.<anonymous>.<no name provided>?"), DEBUG_INFO_SMARTCAST!>z<!>.a.equals(<!DEBUG_INFO_SMARTCAST!>z<!>.a)
        }
    }
}

// TESTCASE NUMBER: 26
fun case_26(a: ((Float) -> Int?)?, b: Float?) {
    if (a != null == true && b != null == true) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!><!DEBUG_INFO_EXPRESSION_TYPE("((kotlin.Float) -> kotlin.Int?)? & (kotlin.Float) -> kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>)<!>
        if (x != null == true) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        }
    }
}

// TESTCASE NUMBER: 27
fun case_27() {
    if (Object.prop_1 == null == true == true == true == true == true == true == true == true == true == true == true == true == true == true)
    else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>Object.prop_1<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!>Object.prop_1<!>.equals(Object.prop_1)
    }
}

//TESTCASE NUMBER: 28
fun case_28(a: DeepObject.A.B.C.D.E.F.G.J?) =
    if (a != null == true == false == false == false == true == false == true == false == false == true == true) {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.x
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.equals(a)
    } else -1

// TESTCASE NUMBER: 29
open class Case29(a: Int?, val b: Float?, private val c: Unit?, protected val d: String?, internal val e: Char?, public val f: Any?) {
    val x: Char? = '.'
    private val y: Unit? = kotlin.Unit
    protected val z: Int? = 12
    public val u: String? = "..."
    val s: Any?
    val v: Int?
    val w: Number?
    val t: String? = if (u != null) this.u else null

    init {
        if (a != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!>.equals(a)
        if (a != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>a<!>

        if (b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>.equals(b)
        if (b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>b<!>
        if (this.b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>this.b<!>
        if (this.b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float"), DEBUG_INFO_SMARTCAST!>this.b<!>.equals(this.b)
        if (this.b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>b<!>
        if (this.b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>.equals(b)
        if (b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>this.b<!>
        if (b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float"), DEBUG_INFO_SMARTCAST!>this.b<!>.equals(b)
        if (b != null || <!SENSELESS_COMPARISON!>this.b != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>.equals(b)
        if (b != null || <!SENSELESS_COMPARISON!>this.b != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>b<!>
        if (b != null || <!SENSELESS_COMPARISON!>this.b != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float"), DEBUG_INFO_SMARTCAST!>this.b<!>.equals(this.b)
        if (b != null || <!SENSELESS_COMPARISON!>this.b != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>this.b<!>

        if (c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>c<!>.equals(c)
        if (c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>c<!>
        if (this.c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit"), DEBUG_INFO_SMARTCAST!>this.c<!>.equals(c)
        if (this.c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>this.c<!>
        if (c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit"), DEBUG_INFO_SMARTCAST!>this.c<!>.equals(this.c)
        if (c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>this.c<!>
        if (this.c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>c<!>.equals(this.c)
        if (this.c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>c<!>
        if (c != null || <!SENSELESS_COMPARISON!>this.c != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>c<!>.equals(c)
        if (c != null || <!SENSELESS_COMPARISON!>this.c != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>c<!>
        if (c != null || <!SENSELESS_COMPARISON!>this.c != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit"), DEBUG_INFO_SMARTCAST!>this.c<!>.equals(this.c)
        if (c != null || <!SENSELESS_COMPARISON!>this.c != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>this.c<!>

        if (d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>d<!>.equals(d)
        if (d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>d<!>
        if (this.d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>this.d<!>.equals(d)
        if (this.d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>this.d<!>
        if (d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>this.d<!>.equals(d)
        if (d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>this.d<!>
        if (this.d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>d<!>.equals(d)
        if (this.d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>d<!>
        if (d != null || <!SENSELESS_COMPARISON!>this.d != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>d<!>.equals(d)
        if (d != null || <!SENSELESS_COMPARISON!>this.d != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>d<!>
        if (d != null || <!SENSELESS_COMPARISON!>this.d != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>this.d<!>.equals(d)
        if (d != null || <!SENSELESS_COMPARISON!>this.d != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>this.d<!>

        if (e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>e<!>.equals(e)
        if (e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>e<!>
        if (this.e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>this.e<!>.equals(e)
        if (this.e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>this.e<!>
        if (this.e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>e<!>.equals(e)
        if (this.e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>e<!>
        if (e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>this.e<!>.equals(e)
        if (e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>this.e<!>
        if (e != null || <!SENSELESS_COMPARISON!>this.e != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>e<!>.equals(e)
        if (e != null || <!SENSELESS_COMPARISON!>this.e != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>e<!>
        if (e != null || <!SENSELESS_COMPARISON!>this.e != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>this.e<!>.equals(e)
        if (e != null || <!SENSELESS_COMPARISON!>this.e != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>this.e<!>

        if (f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>f<!>.equals(f)
        if (f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>f<!>
        if (this.f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>this.f<!>.equals(f)
        if (this.f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>this.f<!>
        if (this.f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>f<!>.equals(f)
        if (this.f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>f<!>
        if (f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>this.f<!>.equals(f)
        if (f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>this.f<!>
        if (f != null || <!SENSELESS_COMPARISON!>this.f != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>f<!>.equals(f)
        if (f != null || <!SENSELESS_COMPARISON!>this.f != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>f<!>
        if (f != null || <!SENSELESS_COMPARISON!>this.f != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>this.f<!>.equals(f)
        if (f != null || <!SENSELESS_COMPARISON!>this.f != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>this.f<!>

        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>
        if (this.x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>this.x<!>.equals(x)
        if (this.x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>this.x<!>
        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>this.x<!>.equals(x)
        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>this.x<!>
        if (this.x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        if (this.x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>
        if (x != null || <!SENSELESS_COMPARISON!>this.x != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        if (x != null || <!SENSELESS_COMPARISON!>this.x != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>
        if (x != null || <!SENSELESS_COMPARISON!>this.x != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>this.x<!>.equals(x)
        if (x != null || <!SENSELESS_COMPARISON!>this.x != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>this.x<!>

        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>y<!>
        if (this.y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit"), DEBUG_INFO_SMARTCAST!>this.y<!>.equals(y)
        if (this.y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>this.y<!>
        if (this.y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
        if (this.y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>y<!>
        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit"), DEBUG_INFO_SMARTCAST!>this.y<!>.equals(y)
        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>this.y<!>
        if (y != null || <!SENSELESS_COMPARISON!>this.y != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
        if (y != null || <!SENSELESS_COMPARISON!>this.y != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>y<!>
        if (y != null || <!SENSELESS_COMPARISON!>this.y != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit"), DEBUG_INFO_SMARTCAST!>this.y<!>.equals(y)
        if (y != null || <!SENSELESS_COMPARISON!>this.y != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>this.y<!>

        if (z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>z<!>.equals(z)
        if (z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>z<!>
        if (this.z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>this.z<!>.equals(z)
        if (this.z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>this.z<!>
        if (z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>this.z<!>.equals(z)
        if (z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>this.z<!>
        if (this.z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>z<!>.equals(z)
        if (this.z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>z<!>
        if (z != null || <!SENSELESS_COMPARISON!>this.z != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>z<!>
        if (z != null || <!SENSELESS_COMPARISON!>this.z != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>z<!>.equals(z)
        if (z != null || <!SENSELESS_COMPARISON!>this.z != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>this.z<!>
        if (z != null || <!SENSELESS_COMPARISON!>this.z != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>this.z<!>.equals(z)

        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>u<!>.equals(u)
        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>u<!>
        if (this.u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>this.u<!>.equals(u)
        if (this.u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>this.u<!>
        if (this.u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>u<!>.equals(u)
        if (this.u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>u<!>
        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>this.u<!>.equals(u)
        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>this.u<!>
        if (u != null || <!SENSELESS_COMPARISON!>this.u != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>u<!>.equals(u)
        if (u != null || <!SENSELESS_COMPARISON!>this.u != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>u<!>
        if (u != null || <!SENSELESS_COMPARISON!>this.u != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>this.u<!>.equals(u)
        if (u != null || <!SENSELESS_COMPARISON!>this.u != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>this.u<!>

        v = 0
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>v<!>.equals(v)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>v<!>
        if (<!SENSELESS_COMPARISON!>v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>v<!>.equals(v)
        if (<!SENSELESS_COMPARISON!>v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>v<!>
        if (<!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>v<!>.equals(v)
        if (<!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>v<!>
        if (<!SENSELESS_COMPARISON!>v != null<!> || <!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>v<!>.equals(v)
        if (<!SENSELESS_COMPARISON!>v != null<!> || <!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>v<!>
        if (<!SENSELESS_COMPARISON!>v != null<!> || <!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>this.v<!>.equals(v)
        if (<!SENSELESS_COMPARISON!>v != null<!> || <!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>this.v<!>

        w = if (<!SENSELESS_COMPARISON!>null != null<!>) 10 else null
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Number?")!>w<!>
        if (w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>w<!>.equals(w)
        if (w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number & kotlin.Number?")!>w<!>
        if (this.w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>w<!>.equals(w)
        if (this.w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number & kotlin.Number?")!>w<!>
        if (w != null || <!SENSELESS_COMPARISON!>this.w != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>w<!>.equals(w)
        if (w != null || <!SENSELESS_COMPARISON!>this.w != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number & kotlin.Number?")!>w<!>
        if (w != null || <!SENSELESS_COMPARISON!>this.w != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>this.w<!>.equals(w)
        if (w != null || <!SENSELESS_COMPARISON!>this.w != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number & kotlin.Number?")!>this.w<!>

        s = null
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing?")!>s<!>.equals(<!DEBUG_INFO_CONSTANT!>s<!>)
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing?")!>s<!>
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>s<!>.equals(s)
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Nothing")!>s<!>
        if (<!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>s<!>.equals(s)
        if (<!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Nothing")!>s<!>
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!> || <!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>s<!>.equals(s)
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!> || <!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Nothing")!>s<!>
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!> || <!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>this.s<!>.equals(s)
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!> || <!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Nothing")!>this.s<!>
    }

    fun test() {
        if (b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>.equals(b)
        if (b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>b<!>

        if (c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>c<!>.equals(c)
        if (c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>c<!>

        if (d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>d<!>.equals(d)
        if (d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>d<!>

        if (e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>e<!>.equals(e)
        if (e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>e<!>

        if (f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>f<!>.equals(f)
        if (f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>f<!>

        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>

        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>y<!>

        if (z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>z<!>.equals(z)
        if (z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>z<!>

        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>u<!>.equals(u)
        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>u<!>

        if (v != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>v<!>.equals(v)
        if (v != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>v<!>

        if (w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>w<!>.equals(w)
        if (w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>w<!>

        if (s != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>s<!>.equals(s)
        if (s != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>s<!>
    }
}

fun case_29(a: Case29) {
    if (a.x !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>a.x<!>.equals(a.x)
    if (a.x !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>a.x<!>
    if (a.b !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float"), DEBUG_INFO_SMARTCAST!>a.b<!>.equals(a.b)
    if (a.b !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>a.b<!>
    if (a.e !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>a.e<!>.equals(a.e)
    if (a.e !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>a.e<!>
    if (a.f !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>a.f<!>.equals(a.f)
    if (a.f !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>a.f<!>
    if (a.v != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>a.v<!>.equals(a.v)
    if (a.v != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>a.v<!>
    if (a.w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!>a.w<!>.equals(a.w)
    if (a.w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>a.w<!>
    if (a.s != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>a.s<!>.equals(a.s)
    if (a.s != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>a.s<!>
}

// TESTCASE NUMBER: 30
sealed class Case30(a: Int?, val b: Float?, private val c: Unit?, protected val d: String?, internal val e: Char?, public val f: Any?) {
    val x: Char? = '.'
    private val y: Unit? = kotlin.Unit
    protected val z: Int? = 12
    public val u: String? = "..."
    val s: Any?
    val v: Int?
    val w: Number?
    val t: String? = if (u != null) this.u else null

    init {
        if (a != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!>.equals(a)
        if (a != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>a<!>

        if (b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>.equals(b)
        if (b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>b<!>
        if (this.b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float"), DEBUG_INFO_SMARTCAST!>this.b<!>.equals(this.b)
        if (this.b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>this.b<!>
        if (this.b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>.equals(b)
        if (this.b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>b<!>
        if (b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float"), DEBUG_INFO_SMARTCAST!>this.b<!>.equals(this.b)
        if (b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>this.b<!>
        if (b != null || <!SENSELESS_COMPARISON!>this.b != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>.equals(b)
        if (b != null || <!SENSELESS_COMPARISON!>this.b != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>b<!>
        if (b != null || <!SENSELESS_COMPARISON!>this.b != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float"), DEBUG_INFO_SMARTCAST!>this.b<!>.equals(this.b)
        if (b != null || <!SENSELESS_COMPARISON!>this.b != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>this.b<!>

        if (c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>c<!>.equals(c)
        if (c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>c<!>
        if (this.c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit"), DEBUG_INFO_SMARTCAST!>this.c<!>.equals(this.c)
        if (this.c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>this.c<!>
        if (c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit"), DEBUG_INFO_SMARTCAST!>this.c<!>.equals(this.c)
        if (c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>this.c<!>
        if (this.c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>c<!>.equals(c)
        if (this.c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>c<!>
        if (c != null || <!SENSELESS_COMPARISON!>this.c != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>c<!>.equals(c)
        if (c != null || <!SENSELESS_COMPARISON!>this.c != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>c<!>
        if (c != null || <!SENSELESS_COMPARISON!>this.c != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit"), DEBUG_INFO_SMARTCAST!>this.c<!>.equals(this.c)
        if (c != null || <!SENSELESS_COMPARISON!>this.c != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>this.c<!>

        if (d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>d<!>.equals(d)
        if (d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>d<!>
        if (this.d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>this.d<!>.equals(this.d)
        if (this.d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>this.d<!>
        if (d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>this.d<!>.equals(this.d)
        if (d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>this.d<!>
        if (this.d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>d<!>.equals(d)
        if (this.d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>d<!>
        if (d != null || <!SENSELESS_COMPARISON!>this.d != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>d<!>.equals(d)
        if (d != null || <!SENSELESS_COMPARISON!>this.d != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>d<!>
        if (d != null || <!SENSELESS_COMPARISON!>this.d != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>this.d<!>.equals(this.d)
        if (d != null || <!SENSELESS_COMPARISON!>this.d != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>this.d<!>

        if (e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>e<!>.equals(e)
        if (e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>e<!>
        if (this.e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>this.e<!>.equals(this.e)
        if (this.e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>this.e<!>
        if (this.e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>e<!>.equals(e)
        if (this.e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>e<!>
        if (e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>this.e<!>.equals(this.e)
        if (e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>this.e<!>
        if (e != null || <!SENSELESS_COMPARISON!>this.e != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>e<!>.equals(e)
        if (e != null || <!SENSELESS_COMPARISON!>this.e != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>e<!>
        if (e != null || <!SENSELESS_COMPARISON!>this.e != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>this.e<!>.equals(this.e)
        if (e != null || <!SENSELESS_COMPARISON!>this.e != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>this.e<!>

        if (f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>f<!>.equals(f)
        if (f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>f<!>
        if (this.f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>this.f<!>.equals(this.f)
        if (this.f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>this.f<!>
        if (this.f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>f<!>.equals(f)
        if (this.f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>f<!>
        if (f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>this.f<!>.equals(this.f)
        if (f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>this.f<!>
        if (f != null || <!SENSELESS_COMPARISON!>this.f != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>f<!>.equals(f)
        if (f != null || <!SENSELESS_COMPARISON!>this.f != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>f<!>
        if (f != null || <!SENSELESS_COMPARISON!>this.f != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>this.f<!>.equals(this.f)
        if (f != null || <!SENSELESS_COMPARISON!>this.f != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>this.f<!>

        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>
        if (this.x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>this.x<!>.equals(this.x)
        if (this.x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>this.x<!>
        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>this.x<!>.equals(this.x)
        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>this.x<!>
        if (this.x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        if (this.x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>
        if (x != null || <!SENSELESS_COMPARISON!>this.x != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        if (x != null || <!SENSELESS_COMPARISON!>this.x != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>
        if (x != null || <!SENSELESS_COMPARISON!>this.x != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>this.x<!>.equals(this.x)
        if (x != null || <!SENSELESS_COMPARISON!>this.x != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>this.x<!>

        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>y<!>
        if (this.y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit"), DEBUG_INFO_SMARTCAST!>this.y<!>.equals(this.y)
        if (this.y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>this.y<!>
        if (this.y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
        if (this.y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>y<!>
        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit"), DEBUG_INFO_SMARTCAST!>this.y<!>.equals(this.y)
        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>this.y<!>
        if (y != null || <!SENSELESS_COMPARISON!>this.y != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
        if (y != null || <!SENSELESS_COMPARISON!>this.y != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>y<!>
        if (y != null || <!SENSELESS_COMPARISON!>this.y != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit"), DEBUG_INFO_SMARTCAST!>this.y<!>.equals(this.y)
        if (y != null || <!SENSELESS_COMPARISON!>this.y != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>this.y<!>

        if (z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>z<!>.equals(z)
        if (z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>z<!>
        if (this.z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>this.z<!>.equals(this.z)
        if (this.z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>this.z<!>
        if (z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>this.z<!>.equals(this.z)
        if (z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>this.z<!>
        if (this.z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>z<!>.equals(z)
        if (this.z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>z<!>
        if (z != null || <!SENSELESS_COMPARISON!>this.z != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>z<!>.equals(z)
        if (z != null || <!SENSELESS_COMPARISON!>this.z != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>z<!>
        if (z != null || <!SENSELESS_COMPARISON!>this.z != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>this.z<!>.equals(this.z)
        if (z != null || <!SENSELESS_COMPARISON!>this.z != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>this.z<!>

        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>u<!>.equals(u)
        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>u<!>
        if (this.u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>this.u<!>.equals(this.u)
        if (this.u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>this.u<!>
        if (this.u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>u<!>.equals(u)
        if (this.u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>u<!>
        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>this.u<!>.equals(this.u)
        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>this.u<!>
        if (u != null || <!SENSELESS_COMPARISON!>this.u != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>u<!>.equals(u)
        if (u != null || <!SENSELESS_COMPARISON!>this.u != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>u<!>
        if (u != null || <!SENSELESS_COMPARISON!>this.u != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>this.u<!>.equals(this.u)
        if (u != null || <!SENSELESS_COMPARISON!>this.u != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>this.u<!>

        v = 0
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>v<!>.equals(v)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>v<!>
        if (<!SENSELESS_COMPARISON!>v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>v<!>.equals(v)
        if (<!SENSELESS_COMPARISON!>v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>v<!>
        if (<!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>v<!>.equals(v)
        if (<!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>v<!>
        if (<!SENSELESS_COMPARISON!>v != null<!> || <!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>v<!>.equals(v)
        if (<!SENSELESS_COMPARISON!>v != null<!> || <!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>v<!>
        if (<!SENSELESS_COMPARISON!>v != null<!> || <!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>this.v<!>.equals(this.v)
        if (<!SENSELESS_COMPARISON!>v != null<!> || <!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>this.v<!>

        w = if (<!SENSELESS_COMPARISON!>null != null<!>) 10 else null
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Number?")!>w<!>
        if (w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>w<!>.equals(w)
        if (w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number & kotlin.Number?")!>w<!>
        if (this.w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>w<!>.equals(w)
        if (this.w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number & kotlin.Number?")!>w<!>
        if (w != null || <!SENSELESS_COMPARISON!>this.w != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>w<!>.equals(w)
        if (w != null || <!SENSELESS_COMPARISON!>this.w != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number & kotlin.Number?")!>w<!>
        if (w != null || <!SENSELESS_COMPARISON!>this.w != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>this.w<!>.equals(this.w)
        if (w != null || <!SENSELESS_COMPARISON!>this.w != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number & kotlin.Number?")!>this.w<!>

        s = null
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing?")!>s<!>.equals(<!DEBUG_INFO_CONSTANT!>s<!>)
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing?")!>s<!>
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>s<!>.equals(s)
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Nothing")!>s<!>
        if (<!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>s<!>.equals(s)
        if (<!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Nothing")!>s<!>
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!> || <!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>s<!>.equals(s)
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!> || <!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Nothing")!>s<!>
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!> || <!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>this.s<!>.equals(this.s)
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!> || <!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Nothing")!>this.s<!>
    }

    fun test() {
        if (b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>.equals(b)
        if (b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>b<!>

        if (c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>c<!>.equals(c)
        if (c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>c<!>

        if (d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>d<!>.equals(d)
        if (d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>d<!>

        if (e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>e<!>.equals(e)
        if (e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>e<!>

        if (f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>f<!>.equals(f)
        if (f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>f<!>

        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>

        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>y<!>

        if (z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>z<!>.equals(z)
        if (z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>z<!>

        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>u<!>.equals(u)
        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>u<!>

        if (v != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>v<!>.equals(v)
        if (v != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>v<!>

        if (w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>w<!>.equals(w)
        if (w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>w<!>

        if (s != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>s<!>.equals(s)
        if (s != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>s<!>
    }
}

fun case_30(a: Case30) {
    if (a.x !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>a.x<!>.equals(a.x)
    if (a.x !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>a.x<!>
    if (a.b !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float"), DEBUG_INFO_SMARTCAST!>a.b<!>.equals(a.b)
    if (a.b !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>a.b<!>
    if (a.e !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>a.e<!>.equals(a.e)
    if (a.e !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>a.e<!>
    if (a.f !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>a.f<!>.equals(a.f)
    if (a.f !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>a.f<!>
    if (a.v != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>a.v<!>.equals(a.v)
    if (a.v != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>a.v<!>
    if (a.w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!>a.w<!>.equals(a.w)
    if (a.w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>a.w<!>
    if (a.s != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>a.s<!>.equals(a.s)
    if (a.s != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>a.s<!>
}

// TESTCASE NUMBER: 31
enum class Case31(a: Int?, val b: Float?, private val c: Unit?, protected val d: String?, internal val e: Char?, public val f: Any?) {
    A(1, 2f, kotlin.Unit, "", ',', null), B(1, 2f, kotlin.Unit, "", ',', null), C(1, 2f, kotlin.Unit, "", ',', null);

    val x: Char? = '.'
    private val y: Unit? = kotlin.Unit
    protected val z: Int? = 12
    public val u: String? = "..."
    val s: Any?
    val v: Int?
    val w: Number?
    val t: String? = if (u != null) this.u else null

    init {
        if (a != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!>.equals(a)
        if (a != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>a<!>

        if (b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>.equals(b)
        if (b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>b<!>
        if (this.b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float"), DEBUG_INFO_SMARTCAST!>this.b<!>.equals(this.b)
        if (this.b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>this.b<!>
        if (this.b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>.equals(b)
        if (this.b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>b<!>
        if (b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float"), DEBUG_INFO_SMARTCAST!>this.b<!>.equals(this.b)
        if (b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>this.b<!>
        if (b != null || <!SENSELESS_COMPARISON!>this.b != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>.equals(b)
        if (b != null || <!SENSELESS_COMPARISON!>this.b != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>b<!>
        if (b != null || <!SENSELESS_COMPARISON!>this.b != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float"), DEBUG_INFO_SMARTCAST!>this.b<!>.equals(this.b)
        if (b != null || <!SENSELESS_COMPARISON!>this.b != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>this.b<!>

        if (c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>c<!>.equals(c)
        if (c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>c<!>
        if (this.c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit"), DEBUG_INFO_SMARTCAST!>this.c<!>.equals(this.c)
        if (this.c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>this.c<!>
        if (c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit"), DEBUG_INFO_SMARTCAST!>this.c<!>.equals(this.c)
        if (c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>this.c<!>
        if (this.c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>c<!>.equals(c)
        if (this.c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>c<!>
        if (c != null || <!SENSELESS_COMPARISON!>this.c != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>c<!>.equals(c)
        if (c != null || <!SENSELESS_COMPARISON!>this.c != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>c<!>
        if (c != null || <!SENSELESS_COMPARISON!>this.c != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit"), DEBUG_INFO_SMARTCAST!>this.c<!>.equals(this.c)
        if (c != null || <!SENSELESS_COMPARISON!>this.c != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>this.c<!>

        if (d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>d<!>.equals(d)
        if (d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>d<!>
        if (this.d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>this.d<!>.equals(this.d)
        if (this.d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>this.d<!>
        if (d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>this.d<!>.equals(this.d)
        if (d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>this.d<!>
        if (this.d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>d<!>.equals(d)
        if (this.d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>d<!>
        if (d != null || <!SENSELESS_COMPARISON!>this.d != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>d<!>.equals(d)
        if (d != null || <!SENSELESS_COMPARISON!>this.d != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>d<!>
        if (d != null || <!SENSELESS_COMPARISON!>this.d != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>this.d<!>.equals(this.d)
        if (d != null || <!SENSELESS_COMPARISON!>this.d != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>this.d<!>

        if (e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>e<!>.equals(e)
        if (e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>e<!>
        if (this.e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>this.e<!>.equals(this.e)
        if (this.e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>this.e<!>
        if (this.e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>e<!>.equals(e)
        if (this.e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>e<!>
        if (e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>this.e<!>.equals(this.e)
        if (e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>this.e<!>
        if (e != null || <!SENSELESS_COMPARISON!>this.e != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>e<!>.equals(e)
        if (e != null || <!SENSELESS_COMPARISON!>this.e != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>e<!>
        if (e != null || <!SENSELESS_COMPARISON!>this.e != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>this.e<!>.equals(this.e)
        if (e != null || <!SENSELESS_COMPARISON!>this.e != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>this.e<!>

        if (f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>f<!>.equals(f)
        if (f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>f<!>
        if (this.f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>this.f<!>.equals(this.f)
        if (this.f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>this.f<!>
        if (this.f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>f<!>.equals(f)
        if (this.f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>f<!>
        if (f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>this.f<!>.equals(this.f)
        if (f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>this.f<!>
        if (f != null || <!SENSELESS_COMPARISON!>this.f != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>f<!>.equals(f)
        if (f != null || <!SENSELESS_COMPARISON!>this.f != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>f<!>
        if (f != null || <!SENSELESS_COMPARISON!>this.f != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>this.f<!>.equals(this.f)
        if (f != null || <!SENSELESS_COMPARISON!>this.f != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>this.f<!>

        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>
        if (this.x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>this.x<!>.equals(this.x)
        if (this.x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>this.x<!>
        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>this.x<!>.equals(this.x)
        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>this.x<!>
        if (this.x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        if (this.x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>
        if (x != null || <!SENSELESS_COMPARISON!>this.x != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        if (x != null || <!SENSELESS_COMPARISON!>this.x != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>
        if (x != null || <!SENSELESS_COMPARISON!>this.x != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>this.x<!>.equals(this.x)
        if (x != null || <!SENSELESS_COMPARISON!>this.x != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>this.x<!>

        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>y<!>
        if (this.y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit"), DEBUG_INFO_SMARTCAST!>this.y<!>.equals(this.y)
        if (this.y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>this.y<!>
        if (this.y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
        if (this.y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>y<!>
        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit"), DEBUG_INFO_SMARTCAST!>this.y<!>.equals(this.y)
        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>this.y<!>
        if (y != null || <!SENSELESS_COMPARISON!>this.y != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
        if (y != null || <!SENSELESS_COMPARISON!>this.y != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>y<!>
        if (y != null || <!SENSELESS_COMPARISON!>this.y != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit"), DEBUG_INFO_SMARTCAST!>this.y<!>.equals(this.y)
        if (y != null || <!SENSELESS_COMPARISON!>this.y != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>this.y<!>

        if (z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>z<!>.equals(z)
        if (z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>z<!>
        if (this.z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>this.z<!>.equals(this.z)
        if (this.z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>this.z<!>
        if (z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>this.z<!>.equals(this.z)
        if (z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>this.z<!>
        if (this.z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>z<!>.equals(z)
        if (this.z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>z<!>
        if (z != null || <!SENSELESS_COMPARISON!>this.z != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>z<!>.equals(z)
        if (z != null || <!SENSELESS_COMPARISON!>this.z != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>z<!>
        if (z != null || <!SENSELESS_COMPARISON!>this.z != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>this.z<!>.equals(this.z)
        if (z != null || <!SENSELESS_COMPARISON!>this.z != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>this.z<!>

        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>u<!>.equals(u)
        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>u<!>
        if (this.u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>this.u<!>.equals(this.u)
        if (this.u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>this.u<!>
        if (this.u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>u<!>.equals(u)
        if (this.u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>u<!>
        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>this.u<!>.equals(this.u)
        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>this.u<!>
        if (u != null || <!SENSELESS_COMPARISON!>this.u != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>u<!>.equals(u)
        if (u != null || <!SENSELESS_COMPARISON!>this.u != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>u<!>
        if (u != null || <!SENSELESS_COMPARISON!>this.u != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>this.u<!>.equals(this.u)
        if (u != null || <!SENSELESS_COMPARISON!>this.u != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>this.u<!>

        v = 0
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>v<!>.equals(v)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>v<!>
        if (<!SENSELESS_COMPARISON!>v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>v<!>.equals(v)
        if (<!SENSELESS_COMPARISON!>v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>v<!>
        if (<!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>v<!>.equals(v)
        if (<!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>v<!>
        if (<!SENSELESS_COMPARISON!>v != null<!> || <!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>v<!>.equals(v)
        if (<!SENSELESS_COMPARISON!>v != null<!> || <!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>v<!>
        if (<!SENSELESS_COMPARISON!>v != null<!> || <!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>this.v<!>.equals(this.v)
        if (<!SENSELESS_COMPARISON!>v != null<!> || <!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>this.v<!>

        w = if (<!SENSELESS_COMPARISON!>null != null<!>) 10 else null
        if (w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>w<!>.equals(w)
        if (w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number & kotlin.Number?")!>w<!>
        if (this.w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>w<!>.equals(w)
        if (this.w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number & kotlin.Number?")!>w<!>
        if (w != null || <!SENSELESS_COMPARISON!>this.w != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>w<!>.equals(w)
        if (w != null || <!SENSELESS_COMPARISON!>this.w != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number & kotlin.Number?")!>w<!>
        if (w != null || <!SENSELESS_COMPARISON!>this.w != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>this.w<!>.equals(this.w)
        if (w != null || <!SENSELESS_COMPARISON!>this.w != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number & kotlin.Number?")!>this.w<!>

        s = null
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing?")!>s<!>.equals(<!DEBUG_INFO_CONSTANT!>s<!>)
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing?")!>s<!>
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>s<!>.equals(s)
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Nothing")!>s<!>
        if (<!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>s<!>.equals(s)
        if (<!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Nothing")!>s<!>
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!> || <!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>s<!>.equals(s)
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!> || <!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Nothing")!>s<!>
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!> || <!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>this.s<!>.equals(this.s)
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!> || <!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Nothing")!>this.s<!>
    }

    fun test() {
        if (b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>.equals(b)
        if (b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>b<!>

        if (c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>c<!>.equals(c)
        if (c != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>c<!>

        if (d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>d<!>.equals(d)
        if (d != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>d<!>

        if (e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>e<!>.equals(e)
        if (e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>e<!>

        if (f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>f<!>.equals(f)
        if (f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>f<!>

        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>

        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>y<!>

        if (z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>z<!>.equals(z)
        if (z != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>z<!>

        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>u<!>.equals(u)
        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>u<!>

        if (v != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>v<!>.equals(v)
        if (v != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>v<!>

        if (w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>w<!>.equals(w)
        if (w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>w<!>

        if (s != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>s<!>.equals(s)
        if (s != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>s<!>
    }
}

fun case_31(a: Case31) {
    if (a.x !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>a.x<!>.equals(a.x)
    if (a.x !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>a.x<!>
    if (a.b !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float"), DEBUG_INFO_SMARTCAST!>a.b<!>.equals(a.b)
    if (a.b !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>a.b<!>
    if (a.e !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>a.e<!>.equals(a.e)
    if (a.e !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>a.e<!>
    if (a.f !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>a.f<!>.equals(a.f)
    if (a.f !== null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>a.f<!>
    if (a.v != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>a.v<!>.equals(a.v)
    if (a.v != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>a.v<!>
    if (a.w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!>a.w<!>.equals(a.w)
    if (a.w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>a.w<!>
    if (a.s != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>a.s<!>.equals(a.s)
    if (a.s != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>a.s<!>

    if (Case31.A.b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float"), DEBUG_INFO_SMARTCAST!>Case31.A.b<!>.equals(Case31.A.b)
    if (Case31.A.b != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>Case31.A.b<!>
    if (Case31.A.e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>Case31.A.e<!>.equals(Case31.A.e)
    if (Case31.A.e != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>Case31.A.e<!>
    if (Case31.A.f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>Case31.A.f<!>.equals(Case31.A.f)
    if (Case31.A.f != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>Case31.A.f<!>
}

// TESTCASE NUMBER: 32
object Case32 {
    val x: Char? = '.'
    private val y: Unit? = kotlin.Unit
    public val u: String? = "..."
    val s: Any?
    val v: Int?
    val w: Number?
    val t: String? = if (u != null) this.u else null

    init {
        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>
        if (this.x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>this.x<!>.equals(this.x)
        if (this.x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>this.x<!>
        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>this.x<!>.equals(this.x)
        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>this.x<!>
        if (this.x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        if (this.x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>
        if (x != null || <!SENSELESS_COMPARISON!>this.x != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        if (x != null || <!SENSELESS_COMPARISON!>this.x != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>
        if (x != null || <!SENSELESS_COMPARISON!>this.x != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>this.x<!>.equals(this.x)
        if (x != null || <!SENSELESS_COMPARISON!>this.x != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>this.x<!>

        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>y<!>
        if (this.y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit"), DEBUG_INFO_SMARTCAST!>this.y<!>.equals(this.y)
        if (this.y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>this.y<!>
        if (this.y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
        if (this.y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>y<!>
        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit"), DEBUG_INFO_SMARTCAST!>this.y<!>.equals(this.y)
        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>this.y<!>
        if (y != null || <!SENSELESS_COMPARISON!>this.y != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
        if (y != null || <!SENSELESS_COMPARISON!>this.y != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>y<!>
        if (y != null || <!SENSELESS_COMPARISON!>this.y != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit"), DEBUG_INFO_SMARTCAST!>this.y<!>.equals(this.y)
        if (y != null || <!SENSELESS_COMPARISON!>this.y != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>this.y<!>

        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>u<!>.equals(u)
        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>u<!>
        if (this.u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>this.u<!>.equals(this.u)
        if (this.u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>this.u<!>
        if (this.u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>u<!>.equals(u)
        if (this.u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>u<!>
        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>this.u<!>.equals(this.u)
        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>this.u<!>
        if (u != null || <!SENSELESS_COMPARISON!>this.u != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>u<!>.equals(u)
        if (u != null || <!SENSELESS_COMPARISON!>this.u != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>u<!>
        if (u != null || <!SENSELESS_COMPARISON!>this.u != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>this.u<!>.equals(this.u)
        if (u != null || <!SENSELESS_COMPARISON!>this.u != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>this.u<!>

        v = 0
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>v<!>.equals(v)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>v<!>
        if (<!SENSELESS_COMPARISON!>v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>v<!>.equals(v)
        if (<!SENSELESS_COMPARISON!>v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>v<!>
        if (<!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>v<!>.equals(v)
        if (<!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>v<!>
        if (<!SENSELESS_COMPARISON!>v != null<!> || <!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>v<!>.equals(v)
        if (<!SENSELESS_COMPARISON!>v != null<!> || <!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>v<!>
        if (<!SENSELESS_COMPARISON!>v != null<!> || <!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>this.v<!>.equals(this.v)
        if (<!SENSELESS_COMPARISON!>v != null<!> || <!SENSELESS_COMPARISON!>this.v != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>this.v<!>

        w = if (<!SENSELESS_COMPARISON!>null != null<!>) 10 else null
        if (w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>w<!>.equals(w)
        if (w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number & kotlin.Number?")!>w<!>
        if (this.w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>w<!>.equals(w)
        if (this.w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number & kotlin.Number?")!>w<!>
        if (w != null || <!SENSELESS_COMPARISON!>this.w != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>w<!>.equals(w)
        if (w != null || <!SENSELESS_COMPARISON!>this.w != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number & kotlin.Number?")!>w<!>
        if (w != null || <!SENSELESS_COMPARISON!>this.w != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>this.w<!>.equals(this.w)
        if (w != null || <!SENSELESS_COMPARISON!>this.w != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number & kotlin.Number?")!>this.w<!>

        s = null
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing?")!>s<!>.equals(<!DEBUG_INFO_CONSTANT!>s<!>)
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing?")!>s<!>
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>s<!>.equals(s)
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Nothing")!>s<!>
        if (<!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>s<!>.equals(s)
        if (<!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Nothing")!>s<!>
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!> || <!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>s<!>.equals(s)
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!> || <!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Nothing")!>s<!>
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!> || <!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>this.s<!>.equals(this.s)
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!> || <!SENSELESS_COMPARISON!>this.s != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Nothing")!>this.s<!>
    }

    fun test() {
        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>

        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>y<!>.equals(y)
        if (y != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>y<!>

        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>u<!>.equals(u)
        if (u != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>u<!>

        if (v != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>v<!>.equals(v)
        if (v != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>v<!>

        if (w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>w<!>.equals(w)
        if (w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>w<!>

        if (s != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>s<!>.equals(s)
        if (s != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>s<!>
    }
}

fun case_32(a: Case32) {
    if (a.x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char"), DEBUG_INFO_SMARTCAST!>a.x<!>.equals(a.x)
    if (a.x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>a.x<!>
    if (a.v != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>a.v<!>.equals(a.v)
    if (a.v != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>a.v<!>
    if (a.w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!>a.w<!>.equals(a.w)
    if (a.w != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>a.w<!>
    if (a.s != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any"), DEBUG_INFO_SMARTCAST!>a.s<!>.equals(a.s)
    if (a.s != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>a.s<!>
}

// TESTCASE NUMBER: 33
fun case_33(a: Int?, b: Int = if (a != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!> else 0) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>b<!>.equals(b)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>b<!>
}
