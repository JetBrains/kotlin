// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 4
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: objects, enumClasses, classes, properties, typealiases
 */

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
    if (<!SENSELESS_COMPARISON!>x === null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Nothing")!>x<!>
    }
}

// TESTCASE NUMBER: 2
fun case_2(x: Nothing) {
    if (<!SENSELESS_COMPARISON!>x <!UNREACHABLE_CODE!>== null<!><!>) <!UNREACHABLE_CODE!>{
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x<!>
    }<!>
}

// TESTCASE NUMBER: 3
fun case_3() {
    if (<!SENSELESS_COMPARISON!>Object.prop_2 != null<!>)
    else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing & kotlin.Number")!>Object.prop_2<!>
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: Char) {
    if (<!SENSELESS_COMPARISON!>x == null<!> && true) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Nothing")!>x<!>
    }
}

// TESTCASE NUMBER: 5
fun case_5() {
    val x: Unit = kotlin.Unit

    if (<!SENSELESS_COMPARISON!>x == null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing & kotlin.Unit")!>x<!>
}

// TESTCASE NUMBER: 6
fun case_6(x: EmptyClass) {
    val y = true

    if (<!SENSELESS_COMPARISON!>x == null<!> && !y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("EmptyClass & kotlin.Nothing")!>x<!>
    }
}

// TESTCASE NUMBER: 7
fun case_7() {
    if (<!SENSELESS_COMPARISON!>anonymousTypeProperty == null<!> || <!SENSELESS_COMPARISON!><!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>anonymousTypeProperty<!> == null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Nothing")!>anonymousTypeProperty<!>
    }
}

// TESTCASE NUMBER: 8
fun case_8(x: TypealiasString) {
    if (<!SENSELESS_COMPARISON!>x == null<!> && <!SENSELESS_COMPARISON!><!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */ & kotlin.Nothing")!>x<!> == null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */ & kotlin.Nothing")!>x<!>
}

// TESTCASE NUMBER: 9
fun case_9(x: TypealiasString) {
    if (<!SENSELESS_COMPARISON!>x != null<!>) {

    } else if (false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */ & kotlin.Nothing")!>x<!>
    }
}

// TESTCASE NUMBER: 10
fun case_10() {
    val a = Class()

    if (<!SENSELESS_COMPARISON!>a.prop_5 != null<!> || true) {
        if (<!SENSELESS_COMPARISON!>a.prop_5 == null<!>) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Nothing")!>a.prop_5<!>
        }
    }
}

// TESTCASE NUMBER: 11
fun case_11(x: TypealiasString, y: TypealiasString) {
    val z: TypealiasString = TypealiasString()

    if (<!SENSELESS_COMPARISON!>x != null<!>) {

    } else {
        if (<!SENSELESS_COMPARISON!>y == null<!>) {
            if (<!SENSELESS_COMPARISON!>stringProperty != null<!>) {
                if (false || false || false || <!SENSELESS_COMPARISON!>z == null<!> || false) {
                    <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */ & kotlin.Nothing")!>x<!>
                }
            }
        }
    }
}

// TESTCASE NUMBER: 12
fun case_12(x: TypealiasString, y: TypealiasString) = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>if (<!SENSELESS_COMPARISON!>x != null<!>) "1"
    else if (<!SENSELESS_COMPARISON!>y !== null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */ & kotlin.Nothing")!>x<!>
    else "-1"<!>

// TESTCASE NUMBER: 13
fun case_13(x: otherpackage.EmptyClass13) =
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>if (<!SENSELESS_COMPARISON!>x != null<!>) {
        1
    } else <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing & otherpackage.EmptyClass13"), NI;DEBUG_INFO_SMARTCAST!>x<!><!>

// TESTCASE NUMBER: 14
class A14 {
    val x: otherpackage.TypealiasString14
    init {
        x = otherpackage.TypealiasString14()
    }
}

fun case_14() {
    val a = A14()

    if (<!SENSELESS_COMPARISON!>a.x == null<!>) {
        if (<!SENSELESS_COMPARISON!>a.x == null<!>) {
            if (<!SENSELESS_COMPARISON!>a.x == null<!>) {
                if (<!SENSELESS_COMPARISON!>a.x === null<!>) {
                    if (<!SENSELESS_COMPARISON!>a.x == null<!>) {
                        if (<!SENSELESS_COMPARISON!>a.x == null<!>) {
                            if (<!SENSELESS_COMPARISON!>a.x == null<!>) {
                                if (<!SENSELESS_COMPARISON!>a.x == null<!>) {
                                    if (<!SENSELESS_COMPARISON!>a.x === null<!>) {
                                        if (<!SENSELESS_COMPARISON!>a.x == null<!>) {
                                            if (<!SENSELESS_COMPARISON!>a.x == null<!>) {
                                                if (<!SENSELESS_COMPARISON!>a.x == null<!>) {
                                                    if (<!SENSELESS_COMPARISON!>a.x == null<!>) {
                                                        if (<!SENSELESS_COMPARISON!>a.x == null<!>) {
                                                            if (<!SENSELESS_COMPARISON!>a.x == null<!>) {
                                                                if (<!SENSELESS_COMPARISON!>a.x === null<!>) {
                                                                    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing & otherpackage.TypealiasString14 /* = kotlin.String */")!>a.x<!>
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
    val <!UNUSED_VARIABLE!>t<!> = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>if (true && <!SENSELESS_COMPARISON!>x != null<!>) "" else {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasString /* = kotlin.String */ & kotlin.Nothing")!>x<!>
    }<!>
}

// TESTCASE NUMBER: 16
fun case_16() {
    <!UNREACHABLE_CODE!>val <!UNUSED_VARIABLE!>x<!>: TypealiasNothing =<!> return

    <!UNREACHABLE_CODE!>if (<!SENSELESS_COMPARISON!>x == null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("TypealiasNothing /* = kotlin.Nothing */")!>x<!>
    }<!>
}

/*
 * TESTCASE NUMBER: 17
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28329
 */
val case_17 = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>if (true && true && <!SENSELESS_COMPARISON!>intProperty != null<!>) 0 else {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>intProperty<!>
}<!>

//TESTCASE NUMBER: 18
fun case_18(a: DeepObject.A.B.C.D.E.F.G.J) {
    if (<!SENSELESS_COMPARISON!>a == null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J & kotlin.Nothing")!>a<!>
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

    if (a != null && <!NI;DEBUG_INFO_SMARTCAST!>a<!>.B19 != null && <!NI;DEBUG_INFO_SMARTCAST!><!NI;DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19 != null && <!NI;DEBUG_INFO_SMARTCAST!><!NI;DEBUG_INFO_SMARTCAST!><!NI;DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19 != null && <!SENSELESS_COMPARISON!><!NI;DEBUG_INFO_SMARTCAST!><!NI;DEBUG_INFO_SMARTCAST!><!NI;DEBUG_INFO_SMARTCAST!><!NI;DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x == null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing & kotlin.Number")!><!NI;DEBUG_INFO_SMARTCAST!><!NI;DEBUG_INFO_SMARTCAST!><!NI;DEBUG_INFO_SMARTCAST!><!NI;DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x<!>
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

    if (<!SENSELESS_COMPARISON!>a.B19.C19.D19 === null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided> & kotlin.Nothing")!>a.B19.C19.D19<!>
    }
}

// TESTCASE NUMBER: 21
fun case_21() {
    if (<!SENSELESS_COMPARISON!>EnumClassWithProperty.B.prop_1 == null<!> || false || false || false || false || false || false || false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Nothing")!>EnumClassWithProperty.B.prop_1<!>
    }
}

// TESTCASE NUMBER: 22
fun case_22(a: (() -> Unit)) {
    if (<!SENSELESS_COMPARISON!>a == null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>a()<!>
    }
}

// TESTCASE NUMBER: 23
fun case_23(a: ((Float) -> Int), b: Float) {
    if (<!SENSELESS_COMPARISON!>a == null<!> && <!SENSELESS_COMPARISON!>b == null<!>) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>a(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Nothing")!>b<!>)<!>
        if (<!SENSELESS_COMPARISON!>x !== null<!>) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x<!>
        }
    }
}

/*
 * TESTCASE NUMBER: 24
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28329
 */
fun case_24(a: ((() -> Unit) -> Unit), b: (() -> Unit)) {
    if (false || false || <!SENSELESS_COMPARISON!>a == null<!> && <!SENSELESS_COMPARISON!>b === null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit) -> kotlin.Unit")!>a<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("() -> kotlin.Unit")!>b<!>
    }
}

// TESTCASE NUMBER: 25
fun case_25(a: (() -> Unit) -> Unit, b: (() -> Unit) -> Unit = if (<!SENSELESS_COMPARISON!>a == null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit) -> kotlin.Unit & kotlin.Nothing")!>a<!> else {{}}) {
    <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit) -> kotlin.Unit")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.Unit) -> kotlin.Unit")!>b<!>
}

// TESTCASE NUMBER: 26
fun case_26(a: Int, b: Int = if (<!SENSELESS_COMPARISON!>a === null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Nothing")!>a<!> else 0) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>b<!>
}
