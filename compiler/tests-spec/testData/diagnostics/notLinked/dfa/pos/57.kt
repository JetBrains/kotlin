// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 57
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-20656
 */
fun case_1(x: Any?) {
    if (x is String) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.String")!>x<!>
        val y = if (true) Class::fun_1 else Class::fun_1
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        val z: String = <!TYPE_MISMATCH, TYPE_MISMATCH!>x<!>
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-20656, KT-17386
 */
fun case_2(x: Any?, b: Boolean?) {
    x!!
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
    val y = when (b) {
        true -> Class::fun_1
        false -> Class::fun_2
        null -> Class::fun_3
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
    val z: Any = <!TYPE_MISMATCH, TYPE_MISMATCH!>x<!>
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-20656
 */
fun case_3(x: Any?, b: Boolean?) {
    x as Int
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Int")!>x<!>
    val y = when (b) {
        else -> Class::fun_1
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
    val z: Int = <!TYPE_MISMATCH, TYPE_MISMATCH!>x<!>
}

// TESTCASE NUMBER: 4
fun case_4(x: Any?, b: Boolean?) {
    x as Int
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Int")!>x<!>
    if (b!!) {
        val m = Class::fun_1
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Int")!>x<!>
    val z: Int = <!DEBUG_INFO_SMARTCAST!>x<!>
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-20656
 */
fun case_5(x: Any?, b: Class) {
    x as Int
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Int")!>x<!>
    val y = if (true) b::fun_1 else b::fun_1
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
    val z: Int = <!TYPE_MISMATCH, TYPE_MISMATCH!>x<!>
}

/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-20656
 */
fun case_6(x: Any?, b: Class) {
    x as Int
    val z1 = x
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Int")!>x<!>
    val y = if (true) b::fun_1 else b::fun_1
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>z1<!>
    val z2: Int = <!TYPE_MISMATCH, TYPE_MISMATCH!>z1<!>
}

/*
 * TESTCASE NUMBER: 7
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-20656
 */
fun case_7_1() {}
fun case_7(x: Any?) {
    if (x is String) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.String")!>x<!>
        val y = if (true) ::case_7_1 else ::case_7_1
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        val z: String = <!TYPE_MISMATCH, TYPE_MISMATCH!>x<!>
    }
}

// TESTCASE NUMBER: 8
fun case_8_1() {}
fun case_8(x: Any?) {
    if (x is String) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.String")!>x<!>
        val m = try {
            ::case_8_1
        } catch (e: Exception) {
            ::case_8_1
        }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.String")!>x<!>
        val z: String = <!DEBUG_INFO_SMARTCAST!>x<!>
    }
}

// TESTCASE NUMBER: 9
fun case_9(x: Any?) {
    if (x is String) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.String")!>x<!>
        val m = try {
            Class::fun_1
        } finally {
            Class::fun_1
        }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.String")!>x<!>
        val z: String = <!DEBUG_INFO_SMARTCAST!>x<!>
    }
}
