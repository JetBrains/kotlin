// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30756
 */
fun case_1(x: Any?) {
    while (true) {
        x ?: return
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>equals<!>(10)
}

// TESTCASE NUMBER: 2
fun case_2(a: Any?) {
    while (true) {
        a ?: return
        a
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>a<!>.<!INAPPLICABLE_CANDIDATE!>equals<!>(10)
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30756, KT-35668
 */
fun case_3(x: Int?) {
    while (true) {
        x ?: return ?: <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>equals<!>(10)
}

// TESTCASE NUMBER: 4
fun case_4(x: Boolean?) {
    while (true && (x == true || x == null)) {
        x ?: return
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>equals<!>(10)
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30756
 */
fun case_5(x: Boolean?) {
    while (true) {
        x ?: x ?: null!!
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>equals<!>(10)
}

// TESTCASE NUMBER: 6
fun case_6(x: Boolean?) {
    while (true) {
        if (x != true) throw Exception()
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>equals<!>(10)
}

// TESTCASE NUMBER: 7
fun case_7(x: Boolean?) {
    while (true) {
        if (!(x === false)) return
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>equals<!>(10)
}

// TESTCASE NUMBER: 8
fun case_8(x: Boolean?) {
    while (x ?: return)

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>.equals(10)
}

// TESTCASE NUMBER: 9
fun case_9(x: Boolean?) {
    while (x ?: return)
    while (x == null)

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>.equals(10)
}

// TESTCASE NUMBER: 10
fun case_10(x: Boolean?) {
    while (true) {
        x ?: return
        while (x) {}
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>equals<!>(10)
}

// TESTCASE NUMBER: 11
fun case_11(x: Boolean?) {
    while (true) {
        x ?: return
        break
        x
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>.equals(10)
}

// TESTCASE NUMBER: 12
fun case_12(x: Boolean?) {
    while (true) {
        x ?: return
        break && x
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>.equals(10)
}

/*
 * TESTCASE NUMBER: 13
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30756
 */
fun case_13(x: Boolean?) {
    while (true) {
        x ?: x!!
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>equals<!>(10)
}

/*
 * TESTCASE NUMBER: 14
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30756
 */
fun case_14(x: Boolean?) {
    do {
        x ?: return
    } while(false)

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>.equals(10)
}

// TESTCASE NUMBER: 15
fun case_15(x: Boolean?) {
    do {
        x ?: return
        println(1)
    } while(false)

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>.equals(10)
}

/*
 * TESTCASE NUMBER: 16
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30756
 */
fun case_16(x: Boolean?) {
    do {
        x ?: x!!
    } while(false)

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>.equals(10)
}

// TESTCASE NUMBER: 17
fun case_17(x: Boolean?, y: Boolean?) {
    loop@ while (true) {
        when (y) {
            true -> x!!
            false -> x!!
            null -> if (true) if (true) if (true) if (true) if (true) when (y) {
                true -> when (y) {
                    else -> if (true) if (true) if (true) if (true) if (true) x!! else x!! else x!! else x!! else x!! else x!!
                }
                false -> x!!
                null -> if (true) if (true) if (true) if (true) if (true) x!! else x!! else x!! else x!! else x!! else x!!
            } else x!! else x!! else x!! else x!! else x!!
        }
        break@loop
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>.not()
}
