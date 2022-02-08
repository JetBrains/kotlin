// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28333
 */
fun case_1(x: Boolean?, y: Boolean?) {
    while (true) {
        y != null || break
        x!!
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 2
fun case_2(x: Boolean?, y: Int?) {
    while (true) {
        break || y == null
        x!!
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean? & kotlin.Boolean")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean? & kotlin.Boolean")!>x<!>.not()
}

// TESTCASE NUMBER: 3
fun case_3(x: Boolean?, y: Boolean?) {
    while (true) {
        if (y == null) {
            break
        }
        x!!
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28333
 */
fun case_4(x: Boolean?, y: Boolean?) {
    while (true) {
        y == null && break
        x!!
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 5
fun case_5(x: Boolean?, y: Boolean?) {
    while (true) {
        y ?: break
        x!!
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 6
fun case_6(x: Boolean?, y: Boolean?) {
    loop@ while (true) {
        when (y) {
            true -> break@loop
            false -> break@loop
            null -> if (true) break@loop else 1
        }
        x!!
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 7
fun case_7(x: Boolean?, y: Boolean?) {
    loop@ while (true) {
        when (y) {
            true -> break@loop
            false -> break@loop
            null -> break@loop
        }
        x!!
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean? & kotlin.Boolean")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean? & kotlin.Boolean")!>x<!>.not()
}

// TESTCASE NUMBER: 8
fun case_8(x: Boolean?, y: Boolean?) {
    loop@ while (true) {
        when (y) {
            true -> x!!
            false -> x!!
            null -> if (true) x!! else break@loop
        }
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}
