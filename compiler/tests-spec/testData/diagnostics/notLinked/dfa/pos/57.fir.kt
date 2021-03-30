// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * ISSUES: KT-20656
 */
fun case_1(x: Any?) {
    if (x is String) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.String")!>x<!>
        val y = if (true) Class::fun_1 else Class::fun_1
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.String")!>x<!>
        val z: String = x
    }
}

/*
 * TESTCASE NUMBER: 2
 * ISSUES: KT-20656, KT-17386
 */
fun case_2(x: Any?, b: Boolean?) {
    x!!
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>x<!>
    val y = when (b) {
        true -> Class::fun_1
        false -> Class::fun_2
        null -> Class::fun_3
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>x<!>
    val z: Any = x
}

/*
 * TESTCASE NUMBER: 3
 * ISSUES: KT-20656
 */
fun case_3(x: Any?, b: Boolean?) {
    x as Int
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>x<!>
    val y = when (b) {
        else -> Class::fun_1
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>x<!>
    val z: Int = x
}

// TESTCASE NUMBER: 4
fun case_4(x: Any?, b: Boolean?) {
    x as Int
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>x<!>
    if (b!!) {
        val m = Class::fun_1
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>x<!>
    val z: Int = x
}

/*
 * TESTCASE NUMBER: 5
 * ISSUES: KT-20656
 */
fun case_5(x: Any?, b: Class) {
    x as Int
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>x<!>
    val y = if (true) b::fun_1 else b::fun_1
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>x<!>
    val z: Int = x
}

/*
 * TESTCASE NUMBER: 6
 * ISSUES: KT-20656
 */
fun case_6(x: Any?, b: Class) {
    x as Int
    val z1 = x
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>x<!>
    val y = if (true) b::fun_1 else b::fun_1
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>z1<!>
    val z2: Int = z1
}

/*
 * TESTCASE NUMBER: 7
 * ISSUES: KT-20656
 */
fun case_7_1() {}
fun case_7(x: Any?) {
    if (x is String) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.String")!>x<!>
        val y = if (true) ::case_7_1 else ::case_7_1
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.String")!>x<!>
        val z: String = x
    }
}

// TESTCASE NUMBER: 8
fun case_8_1() {}
fun case_8(x: Any?) {
    if (x is String) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.String")!>x<!>
        val m = try {
            ::case_8_1
        } catch (e: Exception) {
            ::case_8_1
        }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.String")!>x<!>
        val z: String = x
    }
}

// TESTCASE NUMBER: 9
fun case_9(x: Any?) {
    if (x is String) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.String")!>x<!>
        val m = try {
            Class::fun_1
        } finally {
            Class::fun_1
        }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.String")!>x<!>
        val z: String = x
    }
}
