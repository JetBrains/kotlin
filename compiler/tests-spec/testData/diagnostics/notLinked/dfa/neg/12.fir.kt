// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1() {
    var x: Int? = 11
    x!!
    try {x = null;} finally { <!UNRESOLVED_REFERENCE!><!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int?")!>x<!> += 10<!>; }
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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean? & kotlin.Boolean?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>not<!>()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean? & kotlin.Boolean?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>not<!>()
    }
}

// TESTCASE NUMBER: 4
fun case_4() {
    var x: Boolean? = true
    x as Boolean
    try {
        x = null
    } finally { }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean? & kotlin.Boolean?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>not<!>()
}
