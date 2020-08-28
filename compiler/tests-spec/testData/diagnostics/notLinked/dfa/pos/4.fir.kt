// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

// FILE: other_package.kt

package otherpackage

// TESTCASE NUMBER: 13
class EmptyClass13 {}

// TESTCASE NUMBER: 14
typealias TypealiasString14 = String

// FILE: main.kt

import otherpackage.*

// TESTCASE NUMBER: 1
fun case_1(x: Any) {
    if (x === null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>
    }
}

// TESTCASE NUMBER: 2
fun case_2(x: Nothing) {
    if (x == null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>
    }
}

// TESTCASE NUMBER: 3
fun case_3() {
    if (Object.prop_2 != null)
    else {
        Object.prop_2
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: Char) {
    if (x == null && true) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>x<!>
    }
}

// TESTCASE NUMBER: 5
fun case_5() {
    val x: Unit = kotlin.Unit

    if (x == null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>x<!>
}

// TESTCASE NUMBER: 6
fun case_6(x: EmptyClass) {
    val y = true

    if (x == null && !y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass")!>x<!>
    }
}

// TESTCASE NUMBER: 7
fun case_7() {
    if (anonymousTypeProperty == null || <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous> & <anonymous>")!>anonymousTypeProperty<!> == null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>")!>anonymousTypeProperty<!>
    }
}

// TESTCASE NUMBER: 8
fun case_8(x: TypealiasString) {
    if (x == null && <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString")!>x<!> == null) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString")!>x<!>
}

// TESTCASE NUMBER: 9
fun case_9(x: TypealiasString) {
    if (x != null) {

    } else if (false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString")!>x<!>
    }
}

// TESTCASE NUMBER: 10
fun case_10() {
    val a = Class()

    if (a.prop_5 != null || true) {
        if (a.prop_5 == null) {
            a.prop_5
        }
    }
}

// TESTCASE NUMBER: 11
fun case_11(x: TypealiasString, y: TypealiasString) {
    val z: TypealiasString = TypealiasString()

    if (x != null) {

    } else {
        if (y == null) {
            if (stringProperty != null) {
                if (false || false || false || z == null || false) {
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString")!>x<!>
                }
            }
        }
    }
}

// TESTCASE NUMBER: 12
fun case_12(x: TypealiasString, y: TypealiasString) = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>if (x != null) "1"
    else if (y !== null) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString")!>x<!>
    else "-1"<!>

// TESTCASE NUMBER: 13
fun case_13(x: otherpackage.EmptyClass13) =
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>if (x != null) {
        1
    } else <!DEBUG_INFO_EXPRESSION_TYPE("otherpackage.EmptyClass13")!>x<!><!>

// TESTCASE NUMBER: 14
class A14 {
    val x: otherpackage.TypealiasString14
    init {
        x = otherpackage.TypealiasString14()
    }
}

fun case_14() {
    val a = A14()

    if (a.x == null) {
        if (a.x == null) {
            if (a.x == null) {
                if (a.x === null) {
                    if (a.x == null) {
                        if (a.x == null) {
                            if (a.x == null) {
                                if (a.x == null) {
                                    if (a.x === null) {
                                        if (a.x == null) {
                                            if (a.x == null) {
                                                if (a.x == null) {
                                                    if (a.x == null) {
                                                        if (a.x == null) {
                                                            if (a.x == null) {
                                                                if (a.x === null) {
                                                                    a.x
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
fun case_15(x: TypealiasString) {
    val t = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>if (true && x != null) "" else {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString")!>x<!>
    }<!>
}

// TESTCASE NUMBER: 16
fun case_16() {
    val x: TypealiasNothing = return

    if (x == null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNothing")!>x<!>
    }
}

/*
 * TESTCASE NUMBER: 17
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28329
 */
val case_17 = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>if (true && true && intProperty != null) 0 else {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>intProperty<!>
}<!>

//TESTCASE NUMBER: 18
fun case_18(a: DeepObject.A.B.C.D.E.F.G.J) {
    if (a == null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J")!>a<!>
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
                                    val x: Number = 10
                                }
                            } else null
                        }
                    } else null
                }
            } else null
        }
    } else null

    if (a != null && a.B19 != null && a.B19.C19 != null && a.B19.C19.D19 != null && a.B19.C19.D19.x == null) {
        a.B19.C19.D19.x
    }
}

// TESTCASE NUMBER: 20
fun case_20() {
    val a = object {
        val B19 = object {
            val C19 = object {
                val D19 = object {}
            }
        }
    }

    if (a.B19.C19.D19 === null) {
        a.B19.C19.D19
    }
}

// TESTCASE NUMBER: 21
fun case_21() {
    if (EnumClassWithProperty.B.prop_1 == null || false || false || false || false || false || false || false) {
        EnumClassWithProperty.B.prop_1
    }
}

// TESTCASE NUMBER: 22
fun case_22(a: (() -> Unit)) {
    if (a == null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>a()<!>
    }
}

// TESTCASE NUMBER: 23
fun case_23(a: ((Float) -> Int), b: Float) {
    if (a == null && b == null) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>a(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float")!>b<!>)<!>
        if (x !== null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int")!>x<!>
        }
    }
}

/*
 * TESTCASE NUMBER: 24
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28329
 */
fun case_24(a: ((() -> Unit) -> Unit), b: (() -> Unit)) {
    if (false || false || a == null && b === null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function0<kotlin.Unit>")!>b<!>
    }
}

// TESTCASE NUMBER: 25
fun case_25(a: (() -> Unit) -> Unit, b: (() -> Unit) -> Unit = if (a == null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>")!>a<!> else {{}}) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Function1<kotlin.Function0<kotlin.Unit>, kotlin.Unit>")!>b<!>
}

// TESTCASE NUMBER: 26
fun case_26(a: Int, b: Int = if (a === null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>a<!> else 0) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>b<!>
}
