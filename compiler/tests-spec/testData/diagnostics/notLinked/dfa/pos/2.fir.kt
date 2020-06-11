// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>equals<!>(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>propAny<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>funAny<!>()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 2
fun case_2(a: DeepObject.A.B.C.D.E.F.G.J?) =
    if (false || a != null == true == false == false == false == true == false == true == false == false == true == true || false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.<!INAPPLICABLE_CANDIDATE!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.<!INAPPLICABLE_CANDIDATE!>equals<!>(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.<!INAPPLICABLE_CANDIDATE!>propAny<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.<!INAPPLICABLE_CANDIDATE!>funAny<!>()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.funNullableAny()
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
        val z = <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function0.invoke]")!><!INAPPLICABLE_CANDIDATE!>y<!>()<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<anonymous?>?")!>y<!>.<!INAPPLICABLE_CANDIDATE!>equals<!>(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<anonymous?>?")!>y<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<anonymous?>?")!>y<!>.<!INAPPLICABLE_CANDIDATE!>propAny<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<anonymous?>?")!>y<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<anonymous?>?")!>y<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<anonymous?>?")!>y<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<anonymous?>?")!>y<!>.<!INAPPLICABLE_CANDIDATE!>funAny<!>()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<anonymous?>?")!>y<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<anonymous?>?")!>y<!>.funNullableAny()

        if (z != null || false) {
            <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function0.invoke]")!>z<!>.<!UNRESOLVED_REFERENCE!>a<!>
        }
    }
}

// TESTCASE NUMBER: 4
fun case_4(a: ((Float) -> Int?)?, b: Float?) {
    if (a != null == true && b != null == true || false || false || false || false || false || false || false || false || false) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function1.invoke]")!><!INAPPLICABLE_CANDIDATE!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float?")!>b<!>)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>?")!>a<!>.<!INAPPLICABLE_CANDIDATE!>equals<!>(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>?")!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>?")!>a<!>.<!INAPPLICABLE_CANDIDATE!>propAny<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>?")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>?")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>?")!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>?")!>a<!>.<!INAPPLICABLE_CANDIDATE!>funAny<!>()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>?")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>?")!>a<!>.funNullableAny()

        if (false || x != null == true) {
            <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function1.invoke]")!>x<!>
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
        a.<!INAPPLICABLE_CANDIDATE!>B5<!>.<!UNRESOLVED_REFERENCE!>C5<!>.<!UNRESOLVED_REFERENCE!>D5<!>.<!UNRESOLVED_REFERENCE!>x<!>
        a.<!INAPPLICABLE_CANDIDATE!>B5<!>.<!UNRESOLVED_REFERENCE!>C5<!>.<!UNRESOLVED_REFERENCE!>D5<!>.<!UNRESOLVED_REFERENCE!>x<!>.equals(null)
        a.<!INAPPLICABLE_CANDIDATE!>B5<!>.<!UNRESOLVED_REFERENCE!>C5<!>.<!UNRESOLVED_REFERENCE!>D5<!>.<!UNRESOLVED_REFERENCE!>x<!>.<!INAPPLICABLE_CANDIDATE!>propT<!>
        a.<!INAPPLICABLE_CANDIDATE!>B5<!>.<!UNRESOLVED_REFERENCE!>C5<!>.<!UNRESOLVED_REFERENCE!>D5<!>.<!UNRESOLVED_REFERENCE!>x<!>.propAny
        a.<!INAPPLICABLE_CANDIDATE!>B5<!>.<!UNRESOLVED_REFERENCE!>C5<!>.<!UNRESOLVED_REFERENCE!>D5<!>.<!UNRESOLVED_REFERENCE!>x<!>.<!INAPPLICABLE_CANDIDATE!>propNullableT<!>
        a.<!INAPPLICABLE_CANDIDATE!>B5<!>.<!UNRESOLVED_REFERENCE!>C5<!>.<!UNRESOLVED_REFERENCE!>D5<!>.<!UNRESOLVED_REFERENCE!>x<!>.propNullableAny
        a.<!INAPPLICABLE_CANDIDATE!>B5<!>.<!UNRESOLVED_REFERENCE!>C5<!>.<!UNRESOLVED_REFERENCE!>D5<!>.<!UNRESOLVED_REFERENCE!>x<!>.<!INAPPLICABLE_CANDIDATE!>funT<!>()
        a.<!INAPPLICABLE_CANDIDATE!>B5<!>.<!UNRESOLVED_REFERENCE!>C5<!>.<!UNRESOLVED_REFERENCE!>D5<!>.<!UNRESOLVED_REFERENCE!>x<!>.funAny()
        a.<!INAPPLICABLE_CANDIDATE!>B5<!>.<!UNRESOLVED_REFERENCE!>C5<!>.<!UNRESOLVED_REFERENCE!>D5<!>.<!UNRESOLVED_REFERENCE!>x<!>.<!INAPPLICABLE_CANDIDATE!>funNullableT<!>()
        a.<!INAPPLICABLE_CANDIDATE!>B5<!>.<!UNRESOLVED_REFERENCE!>C5<!>.<!UNRESOLVED_REFERENCE!>D5<!>.<!UNRESOLVED_REFERENCE!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 6
fun case_6(z: Boolean?) {
    if (false || EnumClassWithNullableProperty.B.prop_1 != null && z != null && z) {
        EnumClassWithNullableProperty.B.prop_1
        EnumClassWithNullableProperty.B.prop_1.<!INAPPLICABLE_CANDIDATE!>equals<!>(null)
        EnumClassWithNullableProperty.B.prop_1.propT
        EnumClassWithNullableProperty.B.prop_1.<!INAPPLICABLE_CANDIDATE!>propAny<!>
        EnumClassWithNullableProperty.B.prop_1.propNullableT
        EnumClassWithNullableProperty.B.prop_1.propNullableAny
        EnumClassWithNullableProperty.B.prop_1.funT()
        EnumClassWithNullableProperty.B.prop_1.<!INAPPLICABLE_CANDIDATE!>funAny<!>()
        EnumClassWithNullableProperty.B.prop_1.funNullableT()
        EnumClassWithNullableProperty.B.prop_1.funNullableAny()
    }
}

// TESTCASE NUMBER: 7
fun case_7(a: DeepObject.A.B.C.D.E.F.G.J?) {
    val g = false

    if (a != null && g) {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.funNullableAny()
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
        if (false || false || false || false || a.x != null || false || false || false) {
            if (a.x !== null && true) {
                if (a.x != null && b) {
                    if (a.x != null && b && !b) {
                        if (a.x != null && c != null && !c) {
                            if (a.x !== null && c) {
                                if (a.x != null && b && b && b && b && b && b && b && b && b && b && b) {
                                    if (a.x != null && !b && !b && !b && !b && !b && !b && !b && !b && !b) {
                                        if (a.x !== null && null == null) {
                                            if (a.x != null && null!!) {
                                                if (a.x != null) {
                                                    if (a.x != null) {
                                                        if (a.x !== null) {
                                                            if (a.x != null) {
                                                                if (a.x !== null) {
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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 10
fun case_10(a: DeepObject.A.B.C.D.E.F.G.J?) =
    if (a == null == true == false == false == false == true == false == true == false == false == true == true && true) {
        -1
    } else {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.<!INAPPLICABLE_CANDIDATE!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.<!INAPPLICABLE_CANDIDATE!>equals<!>(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.<!INAPPLICABLE_CANDIDATE!>propAny<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.<!INAPPLICABLE_CANDIDATE!>funAny<!>()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>.funNullableAny()
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
        val z = <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function0.invoke]")!><!INAPPLICABLE_CANDIDATE!>y<!>()<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<anonymous?>?")!>y<!>.<!INAPPLICABLE_CANDIDATE!>equals<!>(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<anonymous?>?")!>y<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<anonymous?>?")!>y<!>.<!INAPPLICABLE_CANDIDATE!>propAny<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<anonymous?>?")!>y<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<anonymous?>?")!>y<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<anonymous?>?")!>y<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<anonymous?>?")!>y<!>.<!INAPPLICABLE_CANDIDATE!>funAny<!>()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<anonymous?>?")!>y<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<anonymous?>?")!>y<!>.funNullableAny()

        if (z != null || b) {

        } else {
            <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function0.invoke]")!>z<!>
        }
    }
}

// TESTCASE NUMBER: 12
fun case_12(a: ((Float) -> Int?)?, b: Float?, c: Boolean?) {
    if (true && a == null == true || b == null == true) {

    } else {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function1.invoke]")!><!INAPPLICABLE_CANDIDATE!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>b<!>)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>?")!>a<!>.<!INAPPLICABLE_CANDIDATE!>equals<!>(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>?")!>a<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>?")!>a<!>.<!INAPPLICABLE_CANDIDATE!>propAny<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>?")!>a<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>?")!>a<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>?")!>a<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>?")!>a<!>.<!INAPPLICABLE_CANDIDATE!>funAny<!>()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>?")!>a<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Float, kotlin.Int?>?")!>a<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>b<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>b<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>b<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>b<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>b<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>b<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>b<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>b<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>b<!>.funNullableAny()
        if (x == null == true || (c != null && !c)) {

        } else {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function1.invoke] & ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function1.invoke]")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function1.invoke] & ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function1.invoke]")!>x<!>.equals(null)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function1.invoke] & ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function1.invoke]")!>x<!>.propT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function1.invoke] & ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function1.invoke]")!>x<!>.propAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function1.invoke] & ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function1.invoke]")!>x<!>.propNullableT
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function1.invoke] & ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function1.invoke]")!>x<!>.propNullableAny
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function1.invoke] & ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function1.invoke]")!>x<!>.funT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function1.invoke] & ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function1.invoke]")!>x<!>.funAny()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function1.invoke] & ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function1.invoke]")!>x<!>.funNullableT()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function1.invoke] & ERROR CLASS: Inapplicable(WRONG_RECEIVER): [kotlin/Function1.invoke]")!>x<!>.funNullableAny()
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
        a.<!INAPPLICABLE_CANDIDATE!>B19<!>.<!UNRESOLVED_REFERENCE!>C19<!>.<!UNRESOLVED_REFERENCE!>D19<!>.<!UNRESOLVED_REFERENCE!>x<!>
        a.<!INAPPLICABLE_CANDIDATE!>B19<!>.<!UNRESOLVED_REFERENCE!>C19<!>.<!UNRESOLVED_REFERENCE!>D19<!>.<!UNRESOLVED_REFERENCE!>x<!>.equals(null)
        a.<!INAPPLICABLE_CANDIDATE!>B19<!>.<!UNRESOLVED_REFERENCE!>C19<!>.<!UNRESOLVED_REFERENCE!>D19<!>.<!UNRESOLVED_REFERENCE!>x<!>.<!INAPPLICABLE_CANDIDATE!>propT<!>
        a.<!INAPPLICABLE_CANDIDATE!>B19<!>.<!UNRESOLVED_REFERENCE!>C19<!>.<!UNRESOLVED_REFERENCE!>D19<!>.<!UNRESOLVED_REFERENCE!>x<!>.propAny
        a.<!INAPPLICABLE_CANDIDATE!>B19<!>.<!UNRESOLVED_REFERENCE!>C19<!>.<!UNRESOLVED_REFERENCE!>D19<!>.<!UNRESOLVED_REFERENCE!>x<!>.<!INAPPLICABLE_CANDIDATE!>propNullableT<!>
        a.<!INAPPLICABLE_CANDIDATE!>B19<!>.<!UNRESOLVED_REFERENCE!>C19<!>.<!UNRESOLVED_REFERENCE!>D19<!>.<!UNRESOLVED_REFERENCE!>x<!>.propNullableAny
        a.<!INAPPLICABLE_CANDIDATE!>B19<!>.<!UNRESOLVED_REFERENCE!>C19<!>.<!UNRESOLVED_REFERENCE!>D19<!>.<!UNRESOLVED_REFERENCE!>x<!>.<!INAPPLICABLE_CANDIDATE!>funT<!>()
        a.<!INAPPLICABLE_CANDIDATE!>B19<!>.<!UNRESOLVED_REFERENCE!>C19<!>.<!UNRESOLVED_REFERENCE!>D19<!>.<!UNRESOLVED_REFERENCE!>x<!>.funAny()
        a.<!INAPPLICABLE_CANDIDATE!>B19<!>.<!UNRESOLVED_REFERENCE!>C19<!>.<!UNRESOLVED_REFERENCE!>D19<!>.<!UNRESOLVED_REFERENCE!>x<!>.<!INAPPLICABLE_CANDIDATE!>funNullableT<!>()
        a.<!INAPPLICABLE_CANDIDATE!>B19<!>.<!UNRESOLVED_REFERENCE!>C19<!>.<!UNRESOLVED_REFERENCE!>D19<!>.<!UNRESOLVED_REFERENCE!>x<!>.funNullableAny()
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
        EnumClassWithNullableProperty.B.prop_1.<!INAPPLICABLE_CANDIDATE!>equals<!>(null)
        EnumClassWithNullableProperty.B.prop_1.propT
        EnumClassWithNullableProperty.B.prop_1.<!INAPPLICABLE_CANDIDATE!>propAny<!>
        EnumClassWithNullableProperty.B.prop_1.propNullableT
        EnumClassWithNullableProperty.B.prop_1.propNullableAny
        EnumClassWithNullableProperty.B.prop_1.funT()
        EnumClassWithNullableProperty.B.prop_1.<!INAPPLICABLE_CANDIDATE!>funAny<!>()
        EnumClassWithNullableProperty.B.prop_1.funNullableT()
        EnumClassWithNullableProperty.B.prop_1.funNullableAny()
    }
}

// TESTCASE NUMBER: 15
fun case_15(a: DeepObject.A.B.C.D.E.F.G.J?) {
    val g = false

    if (true && a != null || g || !g || true || !true) {

    } else {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J?")!>a<!>
    }
}

// TESTCASE NUMBER: 16
fun case_16(b: Boolean, c: Boolean?) {
    val a = Case8_16__2()

    if (a.x != null && false && false && false && false && false && false) {
        if ( a.x == null || false) {
        } else {
            if ( a.x === null && true) {
            } else {
                if (a.x == null || !b) {
                } else {
                    if (a.x == null || b || !b) {
                    } else {
                        if (a.x == null || c == null || !c) {
                        } else {
                            if (a.x === null || c) {
                            } else {
                                if (a.x == null || b || b || b || b || b || b || b || b || b || b || b) {
                                } else {
                                    if (a.x == null || !b || !b || !b || !b || !b || !b || !b || !b || !b) {
                                    } else {
                                        if (a.x === null || null == null) {
                                        } else {
                                            if (a.x == null || null!!) {
                                            } else {
                                                if (a.x == null) {
                                                } else {
                                                    if (a.x == null) {
                                                    } else {
                                                        if (a.x === null) {
                                                        } else {
                                                            if (a.x == null) {
                                                            } else {
                                                                if (a.x === null) {
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
fun case_17(a: Int?, b: Int = if (a != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>a<!> else 0) {
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
fun case_18(a: Int?, b: Int = if (false || a != null || false) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a<!> else 0) {
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
