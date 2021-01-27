// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28370
 */
fun case_1() {
    var x: Boolean? = true
    if (x != null) {
        try {
            throw Exception()
        } catch (e: Exception) {
            x = null
        }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean? & kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean? & kotlin.Boolean?")!>x<!>.<!UNSAFE_CALL!>not<!>()
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28370
 */
fun case_2() {
    var x: Boolean? = true
    if (x != null) {
        try {
            x = null
        } finally { }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean? & kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean? & kotlin.Boolean?")!>x<!>.<!UNSAFE_CALL!>not<!>()
    }
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-29482
 */
fun case_3(x: String?) {
    while (true) {
        try {
            if (x == null) {
                throw Exception()
            }
        } catch (e: Exception) {
            break
        }
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x<!>.<!UNSAFE_CALL!>length<!>
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-29482
 */
fun case_4(x: String?) {
    while (true) {
        try {
            x!!
        } catch (e: Exception) {
            break
        }
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x<!>.<!UNSAFE_CALL!>length<!>
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-29482
 */
fun case_5(x: String?) {
    do {
        try {
            x!!
        } catch (e: Exception) {
            break
        }
    } while (true)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x<!>.<!UNSAFE_CALL!>length<!>
}

/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-29482
 */
fun case_6(x: String?) {
    do {
        try {
            if (x == null) {
                throw Exception()
            }
        } catch (e: Exception) {
            break
        }
    } while (true)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>x<!>.<!UNSAFE_CALL!>length<!>
}
