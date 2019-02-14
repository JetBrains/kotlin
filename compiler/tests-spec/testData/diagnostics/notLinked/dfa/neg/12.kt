
// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 12
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28370
 */

// TESTCASE NUMBER: 1
fun case_1() {
    var x: Int? = 11
    x!!
    try {x = null;} finally { <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!> += 10; }
}

// TESTCASE NUMBER: 2
fun case_2() {
    var x: Boolean? = true
    if (x != null) {
        try {
            throw Exception()
        } catch (e: Exception) {
            x = null
        }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
    }
}

// TESTCASE NUMBER: 3
fun case_3() {
    var x: Boolean? = true
    if (x is Boolean) {
        try {
            throw Exception()
        } catch (e: Exception) {
            x = null
        }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
    }
}

// TESTCASE NUMBER: 4
fun case_4() {
    var x: Boolean? = true
    x as Boolean
    try {
        x = null
    } finally { }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
}