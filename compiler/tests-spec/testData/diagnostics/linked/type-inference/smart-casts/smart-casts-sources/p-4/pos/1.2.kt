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
 * NUMBER: 2
 * DESCRIPTION: Smartcasts from nullability condition (value or reference equality + boolean literals) using if expression and simple types.
 * UNSPECIFIED BEHAVIOR
 * HELPERS: objects, enumClasses
 */

// FILE: other_package.kt

package otherpackage

// TESTCASE NUMBER: 8, 16
class Case8_16__1 {}

// FILE: main.kt

import otherpackage.*

// TESTCASE NUMBER: 8, 16
class Case8_16__2 {
    val x: otherpackage.Case8_16__1?
    init {
        x = otherpackage.Case8_16__1()
    }
}

// TESTCASE NUMBER: 1
fun case_1(x: Any?) {
    if (x != null || false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 2
fun case_2(a: DeepObject.A.B.C.D.E.F.G.J?) =
    if (false || a != null == true == false == false == false == true == false == true == false == false == true == true || false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.x
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.equals(a)
    } else -1

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28328
 */
fun case_3(b: Boolean) {
    val x = {
        if (b) object {
            val a = 10
        } else null
    }

    val y = if (b) x else null

    if (false || false || false || false || y !== null) {
        val z = <!DEBUG_INFO_EXPRESSION_TYPE("case_3.<anonymous>.<no name provided>?")!><!UNSAFE_CALL!>y<!>()<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> case_3.<anonymous>.<no name provided>?)?")!>y<!><!UNSAFE_CALL!>.<!>equals(y)

        if (z != null || false) {
            <!DEBUG_INFO_EXPRESSION_TYPE("case_3.<anonymous>.<no name provided> & case_3.<anonymous>.<no name provided>?"), DEBUG_INFO_SMARTCAST!>z<!>.a
        }
    }
}

// TESTCASE NUMBER: 4
fun case_4(a: ((Float) -> Int?)?, b: Float?) {
    if (a != null == true && b != null == true || false || false || false || false || false || false || false || false || false) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!><!DEBUG_INFO_EXPRESSION_TYPE("((kotlin.Float) -> kotlin.Int?)? & (kotlin.Float) -> kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("((kotlin.Float) -> kotlin.Int?)? & (kotlin.Float) -> kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!>.equals(a)

        if (false || x != null == true) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        }
    }
}

// TESTCASE NUMBER: 5
fun case_5(b: Boolean) {
    val a = if (b) {
        object {
            val B5 = if (b) {
                object {
                    val C5 = if (b) {
                        object {
                            val D5 = if (b) {
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

    if (a != null && <!DEBUG_INFO_SMARTCAST!>a<!>.B5 != null && <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B5<!>.C5 != null && <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B5<!>.C5<!>.D5 != null && <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B5<!>.C5<!>.D5<!>.x != null && b || false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B5<!>.C5<!>.D5<!>.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B5<!>.C5<!>.D5<!>.x<!>.equals(<!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B5<!>.C5<!>.D5<!>.x)
    }
}

// TESTCASE NUMBER: 6
fun case_6(z: Boolean?) {
    if (false || EnumClassWithNullableProperty.B.prop_1 != null && z != null && <!DEBUG_INFO_SMARTCAST!>z<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>EnumClassWithNullableProperty.B.prop_1<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int"), DEBUG_INFO_SMARTCAST!>EnumClassWithNullableProperty.B.prop_1<!>.equals(EnumClassWithNullableProperty.B.prop_1)
    }
}

// TESTCASE NUMBER: 7
fun case_7(a: DeepObject.A.B.C.D.E.F.G.J?) {
    val g = false

    if (a != null && g) {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.equals(a)
    }
}

/*
 * TESTCASE NUMBER: 8
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28329
 */
fun case_8(b: Boolean, c: Boolean?) {
    val a = Case8_16__2()

    if (a.x !== null && false) {
        if (false || false || false || false || <!SENSELESS_COMPARISON!>a.x != null<!> || false || false || false) {
            if (<!SENSELESS_COMPARISON!>a.x !== null<!> && true) {
                if (<!SENSELESS_COMPARISON!>a.x != null<!> && b) {
                    if (<!SENSELESS_COMPARISON!>a.x != null<!> && b && !b) {
                        if (<!SENSELESS_COMPARISON!>a.x != null<!> && c != null && !<!DEBUG_INFO_SMARTCAST!>c<!>) {
                            if (<!SENSELESS_COMPARISON!>a.x !== null<!> && <!DEBUG_INFO_SMARTCAST!>c<!>) {
                                if (<!SENSELESS_COMPARISON!>a.x != null<!> && b && b && b && b && b && b && b && b && b && b && b) {
                                    if (<!SENSELESS_COMPARISON!>a.x != null<!> && !b && !b && !b && !b && !b && !b && !b && !b && !b) {
                                        if (<!SENSELESS_COMPARISON!>a.x !== null<!> && <!SENSELESS_COMPARISON!>null == null<!>) {
                                            if (<!SENSELESS_COMPARISON!>a.x != null<!> && null!!) {
                                                if (<!SENSELESS_COMPARISON!>a.x != null<!>) {
                                                    if (<!SENSELESS_COMPARISON!>a.x != null<!>) {
                                                        if (<!SENSELESS_COMPARISON!>a.x !== null<!>) {
                                                            if (<!SENSELESS_COMPARISON!>a.x != null<!>) {
                                                                if (<!SENSELESS_COMPARISON!>a.x !== null<!>) {
                                                                    <!DEBUG_INFO_EXPRESSION_TYPE("otherpackage.Case8_16__1 & otherpackage.Case8_16__1?")!>a.x<!>
                                                                    <!DEBUG_INFO_EXPRESSION_TYPE("otherpackage.Case8_16__1"), DEBUG_INFO_SMARTCAST!>a.x<!>.equals(a.x)
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

// TESTCASE NUMBER: 9
fun case_9(x: Any?) {
    if (x == null || false || false || false || false || false || false) {

    } else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 10
fun case_10(a: DeepObject.A.B.C.D.E.F.G.J?) =
    if (a == null == true == false == false == false == true == false == true == false == false == true == true && true) {
        -1
    } else {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.x
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?"), DEBUG_INFO_SMARTCAST!>a<!>.equals(a)
    }

/*
 * TESTCASE NUMBER: 11
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28328
 */
fun case_11(b: Boolean) {
    val x = {
        if (b) object {
            val a = 10
        } else null
    }

    val y = if (b) x else null

    if (y === null && true) else {
        val z = <!DEBUG_INFO_EXPRESSION_TYPE("case_11.<anonymous>.<no name provided>?")!><!UNSAFE_CALL!>y<!>()<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> case_11.<anonymous>.<no name provided>?)?")!>y<!><!UNSAFE_CALL!>.<!>equals(y)

        if (z != null || b) {

        } else {
            <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("case_11.<anonymous>.<no name provided>? & kotlin.Nothing?")!>z<!>
        }
    }
}

// TESTCASE NUMBER: 12
fun case_12(a: ((Float) -> Int?)?, b: Float?, c: Boolean?) {
    if (true && a == null == true || b == null == true) {

    } else {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!><!DEBUG_INFO_EXPRESSION_TYPE("((kotlin.Float) -> kotlin.Int?)? & (kotlin.Float) -> kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("((kotlin.Float) -> kotlin.Int?)? & (kotlin.Float) -> kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!>.equals(a)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>.equals(b)
        if (x == null == true || (c != null && !<!DEBUG_INFO_SMARTCAST!>c<!>)) {

        } else {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
        }
    }
}

// TESTCASE NUMBER: 13
fun case_13(b: Boolean, c: Boolean, d: Boolean) {
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

    if ((a == null || <!DEBUG_INFO_SMARTCAST!>a<!>.B19 == null || <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19 == null || <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19 == null || <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x == null || b || c || !d) && true) {

    } else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x<!>.equals(<!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x)
    }
}

/*
 * TESTCASE NUMBER: 14
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28329
 */
fun case_14(z: Boolean?) {
    if (true && true && true && true && EnumClassWithNullableProperty.B.prop_1 != null || z != null || <!ALWAYS_NULL!>z<!>!! && true && true) {

    } else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>EnumClassWithNullableProperty.B.prop_1<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>EnumClassWithNullableProperty.B.prop_1<!><!UNSAFE_CALL!>.<!>equals(EnumClassWithNullableProperty.B.prop_1)
    }
}

// TESTCASE NUMBER: 15
fun case_15(a: DeepObject.A.B.C.D.E.F.G.J?) {
    val g = false

    if (true && a != null || g || !g || true || !true) {

    } else {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J? & kotlin.Nothing?")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J? & kotlin.Nothing?")!>a<!>.equals(<!DEBUG_INFO_CONSTANT!>a<!>)
    }
}

// TESTCASE NUMBER: 16
fun case_16(b: Boolean, c: Boolean?) {
    val a = Case8_16__2()

    if (a.x != null && false && false && false && false && false && false) {
        if ( <!SENSELESS_COMPARISON!>a.x == null<!> || false) {
        } else {
            if ( <!SENSELESS_COMPARISON!>a.x === null<!> && true) {
            } else {
                if (<!SENSELESS_COMPARISON!>a.x == null<!> || !b) {
                } else {
                    if (<!SENSELESS_COMPARISON!>a.x == null<!> || b || !b) {
                    } else {
                        if (<!SENSELESS_COMPARISON!>a.x == null<!> || c == null || !<!DEBUG_INFO_SMARTCAST!>c<!>) {
                        } else {
                            if (<!SENSELESS_COMPARISON!>a.x === null<!> || <!DEBUG_INFO_SMARTCAST!>c<!>) {
                            } else {
                                if (<!SENSELESS_COMPARISON!>a.x == null<!> || b || b || b || b || b || b || b || b || b || b || b) {
                                } else {
                                    if (<!SENSELESS_COMPARISON!>a.x == null<!> || !b || !b || !b || !b || !b || !b || !b || !b || !b) {
                                    } else {
                                        if (<!SENSELESS_COMPARISON!>a.x === null<!> || <!SENSELESS_COMPARISON!>null == null<!>) {
                                        } else {
                                            if (<!SENSELESS_COMPARISON!>a.x == null<!> || null!!) {
                                            } else {
                                                if (<!SENSELESS_COMPARISON!>a.x == null<!>) {
                                                } else {
                                                    if (<!SENSELESS_COMPARISON!>a.x == null<!>) {
                                                    } else {
                                                        if (<!SENSELESS_COMPARISON!>a.x === null<!>) {
                                                        } else {
                                                            if (<!SENSELESS_COMPARISON!>a.x == null<!>) {
                                                            } else {
                                                                if (<!SENSELESS_COMPARISON!>a.x === null<!>) {
                                                                } else {
                                                                    <!DEBUG_INFO_EXPRESSION_TYPE("otherpackage.Case8_16__1 & otherpackage.Case8_16__1?")!>a.x<!>
                                                                    <!DEBUG_INFO_EXPRESSION_TYPE("otherpackage.Case8_16__1"), DEBUG_INFO_SMARTCAST!>a.x<!>.equals(a.x)
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

// TESTCASE NUMBER: 17
fun case_17(a: Int?, b: Int = if (a != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!> else 0) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>b<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>b<!>.equals(b)
}

// TESTCASE NUMBER: 18
fun case_18(a: Int?, b: Int = if (false || a != null || false) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!> else 0) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>b<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>b<!>.equals(b)
}
