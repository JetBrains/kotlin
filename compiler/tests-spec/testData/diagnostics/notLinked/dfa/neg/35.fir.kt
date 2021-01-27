// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-18130
 */
fun case_1() {
    var x: String?
    x = "Test"
    println("${if (true) x = null else 1}")
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.String?")!>x<!>.<!UNSAFE_CALL!>length<!>
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-18130
 */
fun case_2() {
    var x: String?
    x = "Test"
    println("${try { x = null } finally { }}")
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.String?")!>x<!>.<!UNSAFE_CALL!>length<!>
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-18130
 */
fun case_3() {
    var x: String?
    x = "Test"
    println("${try {  } finally { x = null }}")
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.String?")!>x<!>.<!UNSAFE_CALL!>length<!>
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-18130
 */
fun case_4() {
    var x: String?
    x = "Test"
    println("${try { x = null } catch (e: Exception) { } finally { }}")
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.String?")!>x<!>.<!UNSAFE_CALL!>length<!>
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-18130
 */
fun case_5() {
    var x: String?
    x = "Test"
    println("${try { } catch (e: Exception) { x = null } finally {  }}")
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.String?")!>x<!>.<!UNSAFE_CALL!>length<!>
}

/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-18130
 */
fun case_6() {
    var x: String?
    x = "Test"
    println("${try { } catch (e: Exception) { } finally { x = null }}")
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.String?")!>x<!>.<!UNSAFE_CALL!>length<!>
}

/*
 * TESTCASE NUMBER: 7
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-18130
 */
fun case_7() {
    var x: String?
    x = "Test"
    println("${try { x = null } catch (e: Exception) { }}")
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.String?")!>x<!>.<!UNSAFE_CALL!>length<!>
}

/*
 * TESTCASE NUMBER: 8
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-18130
 */
fun case_8() {
    var x: String?
    x = "Test"
    println("${try { } catch (e: Exception) { x = null }}")
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.String?")!>x<!>.<!UNSAFE_CALL!>length<!>
}

/*
 * TESTCASE NUMBER: 9
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-18130
 */
fun case_9() {
    var x: String?
    x = "Test"
    println("${when (null) { else -> x = null } }")
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.String?")!>x<!>.<!UNSAFE_CALL!>length<!>
}
