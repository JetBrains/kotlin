// DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 2
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: objects, enumClasses, properties, functions
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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 2
fun case_2(a: DeepObject.A.B.C.D.E.F.G.J?) =
    if (false || a != null == true == false == false == false == true == false == true == false == false == true == true || false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.x
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.funNullableAny()
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
        val z = <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!><!UNSAFE_IMPLICIT_INVOKE_CALL!>y<!>()<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<<anonymous>?>?")!>y<!><!UNSAFE_CALL!>.<!>equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<<anonymous>?>?")!>y<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<<anonymous>?>?")!>y<!><!UNSAFE_CALL!>.<!>propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<<anonymous>?>?")!>y<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<<anonymous>?>?")!>y<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<<anonymous>?>?")!>y<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<<anonymous>?>?")!>y<!><!UNSAFE_CALL!>.<!>funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<<anonymous>?>?")!>y<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<<anonymous>?>?")!>y<!>.funNullableAny()

        if (z != null || false) {
            <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>")!>z<!>.a
        }
    }
}

// TESTCASE NUMBER: 4
fun case_4(a: ((Float) -> Int?)?, b: Float?) {
    if (a != null == true && b != null == true || false || false || false || false || false || false || false || false || false) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float")!>b<!>)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>")!>a<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>")!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>")!>a<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>")!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>")!>a<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>")!>a<!>.funNullableAny()

        if (false || x != null == true) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x<!>
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

    if (a != null && a.B5 != null && a.B5.C5 != null && a.B5.C5.D5 != null && a.B5.C5.D5.x != null && b || false) {
        a.B5.C5.D5.x
        a.B5.C5.D5.x.equals(null)
        a.B5.C5.D5.x.propT
        a.B5.C5.D5.x.propAny
        a.B5.C5.D5.x.propNullableT
        a.B5.C5.D5.x.propNullableAny
        a.B5.C5.D5.x.funT()
        a.B5.C5.D5.x.funAny()
        a.B5.C5.D5.x.funNullableT()
        a.B5.C5.D5.x.funNullableAny()
    }
}

// TESTCASE NUMBER: 6
fun case_6(z: Boolean?) {
    if (false || EnumClassWithNullableProperty.B.prop_1 != null && z != null && z) {
        EnumClassWithNullableProperty.B.prop_1
        EnumClassWithNullableProperty.B.prop_1.equals(null)
        EnumClassWithNullableProperty.B.prop_1.propT
        EnumClassWithNullableProperty.B.prop_1.propAny
        EnumClassWithNullableProperty.B.prop_1.propNullableT
        EnumClassWithNullableProperty.B.prop_1.propNullableAny
        EnumClassWithNullableProperty.B.prop_1.funT()
        EnumClassWithNullableProperty.B.prop_1.funAny()
        EnumClassWithNullableProperty.B.prop_1.funNullableT()
        EnumClassWithNullableProperty.B.prop_1.funNullableAny()
    }
}

// TESTCASE NUMBER: 7
fun case_7(a: DeepObject.A.B.C.D.E.F.G.J?) {
    val g = false

    if (a != null && g) {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.funNullableAny()
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
                        if (<!SENSELESS_COMPARISON!>a.x != null<!> && c != null && !c) {
                            if (<!SENSELESS_COMPARISON!>a.x !== null<!> && c) {
                                if (<!SENSELESS_COMPARISON!>a.x != null<!> && b && b && b && b && b && b && b && b && b && b && b) {
                                    if (<!SENSELESS_COMPARISON!>a.x != null<!> && !b && !b && !b && !b && !b && !b && !b && !b && !b) {
                                        if (<!SENSELESS_COMPARISON!>a.x !== null<!> && <!SENSELESS_COMPARISON!>null == null<!>) {
                                            if (<!SENSELESS_COMPARISON!>a.x != null<!> && null!!) {
                                                if (<!SENSELESS_COMPARISON!>a.x != null<!>) {
                                                    if (<!SENSELESS_COMPARISON!>a.x != null<!>) {
                                                        if (<!SENSELESS_COMPARISON!>a.x !== null<!>) {
                                                            if (<!SENSELESS_COMPARISON!>a.x != null<!>) {
                                                                if (<!SENSELESS_COMPARISON!>a.x !== null<!>) {
                                                                    a.x
                                                                    a.x.equals(null)
                                                                    a.x.propT
                                                                    a.x.propAny
                                                                    a.x.propNullableT
                                                                    a.x.propNullableAny
                                                                    a.x.funT()
                                                                    a.x.funAny()
                                                                    a.x.funNullableT()
                                                                    a.x.funNullableAny()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 10
fun case_10(a: DeepObject.A.B.C.D.E.F.G.J?) =
    if (a == null == true == false == false == false == true == false == true == false == false == true == true && true) {
        -1
    } else {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.x
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>.funNullableAny()
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
        val z = <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>?")!>y()<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<<anonymous>?>")!>y<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<<anonymous>?>")!>y<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<<anonymous>?>")!>y<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<<anonymous>?>")!>y<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<<anonymous>?>")!>y<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<<anonymous>?>")!>y<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<<anonymous>?>")!>y<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<<anonymous>?>")!>y<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<<anonymous>?>")!>y<!>.funNullableAny()

        if (z != null || b) {

        } else {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>z<!>
        }
    }
}

// TESTCASE NUMBER: 12
fun case_12(a: ((Float) -> Int?)?, b: Float?, c: Boolean?) {
    if (true && a == null == true || b == null == true) {

    } else {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float")!>b<!>)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>")!>a<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>")!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>")!>a<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>")!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>")!>a<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>")!>a<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float")!>b<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float")!>b<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float")!>b<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float")!>b<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float")!>b<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float")!>b<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float")!>b<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float")!>b<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float")!>b<!>.funNullableAny()
        if (x == null == true || (c != null && !c)) {

        } else {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x<!>.funNullableAny()
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

    if ((a == null || a.B19 == null || a.B19.C19 == null || a.B19.C19.D19 == null || a.B19.C19.D19.x == null || b || c || !d) && true) {

    } else {
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

/*
 * TESTCASE NUMBER: 14
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28329
 */
fun case_14(z: Boolean?) {
    if (true && true && true && true && EnumClassWithNullableProperty.B.prop_1 != null || z != null || z!! && true && true) {

    } else {
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

// TESTCASE NUMBER: 15
fun case_15(a: DeepObject.A.B.C.D.E.F.G.J?) {
    val g = false

    if (true && a != null || g || !g || true || !true) {

    } else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>a<!>
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
                        if (<!SENSELESS_COMPARISON!>a.x == null<!> || c == null || !c) {
                        } else {
                            if (<!SENSELESS_COMPARISON!>a.x === null<!> || c) {
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
                                                                    a.x
                                                                    a.x.equals(null)
                                                                    a.x.propT
                                                                    a.x.propAny
                                                                    a.x.propNullableT
                                                                    a.x.propNullableAny
                                                                    a.x.funT()
                                                                    a.x.funAny()
                                                                    a.x.funNullableT()
                                                                    a.x.funNullableAny()
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
fun case_17(a: Int?, b: Int = if (a != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>a<!> else 0) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a<!>
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

// TESTCASE NUMBER: 18
fun case_18(a: Int?, b: Int = if (false || a != null || false) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>a<!> else 0) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a<!>
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
