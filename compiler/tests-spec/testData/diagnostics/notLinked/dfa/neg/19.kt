// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 19
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28489
 */
fun case_1(x: Boolean?) {
    l1@ while (true) {
        l2@ while (true || x!!) {
            break@l1
        }
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28489
 */
fun case_2(x: Boolean?) {
    l1@ while (true) {
        l2@ do {
            break@l1
        } while (<!UNREACHABLE_CODE!>true || x!!<!>)
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28489
 */
fun case_3(x: Boolean?) {
    l1@ do {
        l2@ do {
            break@l1
        } while (<!UNREACHABLE_CODE!>true || x!!<!>)
    } while (<!UNREACHABLE_CODE!>true<!>)

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28489
 */
fun case_4(x: Boolean?) {
    l1@ do {
        l2@ do {
            break@l1
        } while (<!UNREACHABLE_CODE!>x!!<!>)
    } while (<!UNREACHABLE_CODE!>true<!>)

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
}

// TESTCASE NUMBER: 5
fun case_5(<!UNUSED_PARAMETER!>x<!>: Boolean?) {
    l1@ do {
        l2@ do {
            break@l2
        } while (<!UNREACHABLE_CODE!>x!!<!>)
    } while (true)

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?"), UNREACHABLE_CODE!>x<!>
    <!UNREACHABLE_CODE!><!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()<!>
}

// TESTCASE NUMBER: 6
fun case_6(x: Boolean?) {
    l1@ do {
        l2@ do {
            break@l1
        } while (<!UNREACHABLE_CODE!>true<!>)
    } while (<!UNREACHABLE_CODE!>x!!<!>)

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 7
fun case_7(x: Boolean?) {
    l1@ while (true) {
        l2@ while (true || x!!) {
            break@l2
        }
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?"), UNREACHABLE_CODE!>x<!>
    <!UNREACHABLE_CODE!><!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()<!>
}

/*
 * TESTCASE NUMBER: 8
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28489
 */
fun case_8(x: Boolean?) {
    l1@ while (true || x!!) {
        l2@ while (true) {
            break@l2
        }
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
}

// TESTCASE NUMBER: 9
fun case_9(x: Boolean?) {
    l1@ while (true) {
        break@l1
        <!UNREACHABLE_CODE!>l2@ while (x!!) {

        }<!>
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

/*
 * TESTCASE NUMBER: 10
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28489
 */
fun case_10(x: Boolean?) {
    l1@ while (true) {
        l2@ <!UNREACHABLE_CODE!>while (<!>break@l1 <!UNREACHABLE_CODE!>|| x!!) {

        }<!>
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
}

// TESTCASE NUMBER: 11
fun case_11(x: Boolean?) {
    l1@ while (true) {
        l2@ <!UNREACHABLE_CODE!>while (<!>break@l1 <!UNREACHABLE_CODE!>&& x!!) {

        }<!>
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 12
fun case_12(x: Boolean?) {
    while (true) {
        break <!UNREACHABLE_CODE!>|| x!!<!>
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 13
fun case_13(x: Boolean?) {
    while (true) {
        break <!UNREACHABLE_CODE!>&& x!!<!>
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 14
fun case_14(x: Boolean?) {
    do {
        break <!UNREACHABLE_CODE!>|| x!!<!>
    } while (<!UNREACHABLE_CODE!>true<!>)

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 15
fun case_15(x: Boolean?) {
    do {
        break <!UNREACHABLE_CODE!>&& x!!<!>
    } while (<!UNREACHABLE_CODE!>true<!>)

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 16
fun case_16(x: Boolean?) {
    do {
        break
    } while (<!UNREACHABLE_CODE!>x!!<!>)

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

/*
 * TESTCASE NUMBER: 17
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28489
 */
fun case_17(x: Boolean?) {
    l1@ while (true) {
        l2@ while (true) {
            l3@ <!UNREACHABLE_CODE!>while (<!>break@l2 <!UNREACHABLE_CODE!>|| x!!) {

            }<!>
        }
        break
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
}

/*
 * TESTCASE NUMBER: 18
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28489
 */
fun case_18(x: Boolean?) {
    l1@ while (true) {
        l2@ while (true) {
            l3@ <!UNREACHABLE_CODE!>while (<!>break@l1 <!UNREACHABLE_CODE!>|| x!!) {

            }<!>
        }
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
}

// TESTCASE NUMBER: 19
fun case_19(x: Boolean?) {
    l1@ while (true) {
        l2@ while (true) {
            l3@ <!UNREACHABLE_CODE!>while (<!>break@l1 <!UNREACHABLE_CODE!>&& x!!) {

            }<!>
        }
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 20
fun case_20(x: Boolean?) {
    l1@ while (true) {
        l2@ while (true) {
            l3@ <!UNREACHABLE_CODE!>while (<!>break@l2 <!UNREACHABLE_CODE!>&& x!!) {

            }<!>
        }
        break
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 21
fun case_21(x: Boolean?) {
    for (i in listOf(1, 2, 3)) {
        break <!UNREACHABLE_CODE!>|| x!!<!>
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 22
fun case_22(x: Boolean?) {
    for (i in listOf(1, 2, 3)) {
        break <!UNREACHABLE_CODE!>&& x!!<!>
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 23
fun case_23(x: Boolean?) {
    l1@ for (i in listOf(1, 2, 3)) {
        l2@ for (j in listOf(1, 2, 3)) {
            break@l1 <!UNREACHABLE_CODE!>|| x!!<!>
        }
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 24
fun case_24(x: Boolean?) {
    l1@ for (i in listOf(1, 2, 3)) {
        l2@ for (j in listOf(1, 2, 3)) {
            true || x!!
            break@l1
        }
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 25
fun case_25(x: Boolean?) {
    l1@ for (i in listOf(1, 2, 3)) {
        l2@ <!UNREACHABLE_CODE!>for (j in listOf(<!>true || x!!, break@l1 <!UNREACHABLE_CODE!>|| x!!, x!!)) {
            break@l1
        }<!>
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

/*
 * TESTCASE NUMBER: 26
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30867
 */
fun case_26(x: Boolean?) {
    while (true) {
        <!UNREACHABLE_CODE!>for (i in listOf(<!>break, <!UNREACHABLE_CODE!>x!!, x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)) {

        }<!>
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
}

/*
 * TESTCASE NUMBER: 27
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30868
 */
fun case_27(x: Int?, y: Class) {
    while (true) {
        y<!UNREACHABLE_CODE!>[<!>break, <!UNREACHABLE_CODE!>x!!]<!>
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.inv()
}

/*
 * TESTCASE NUMBER: 28
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30868
 */
fun case_28(x: Int?, y: List<List<Int>>) {
    while (true) {
        y[break]<!UNREACHABLE_CODE!>[x!!]<!>
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.inv()
}
