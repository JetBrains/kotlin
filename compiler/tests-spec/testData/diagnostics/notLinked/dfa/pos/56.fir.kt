// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 56
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1(x: Class?) {
    if (x != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction0<kotlin.Function1<kotlin.Int, kotlin.Function1<kotlin.Int, kotlin.Int>>>")!>x::fun_1<!>
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-17386
 */
fun case_2(x: Class?) {
    val y = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction0<kotlin.Function1<kotlin.Int, kotlin.Function1<kotlin.Int, kotlin.Int>>>?")!>if (x != null) x::fun_1 else null<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction0<kotlin.Function1<kotlin.Int, kotlin.Function1<kotlin.Int, kotlin.Int>>>?")!>y<!>
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-17386
 */
fun case_3(x: Class?) {
    val y = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<Class, kotlin.Function1<kotlin.Int, kotlin.Function1<kotlin.Int, kotlin.Int>>>?")!>if (x != null) Class::fun_1 else null<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<Class, kotlin.Function1<kotlin.Int, kotlin.Function1<kotlin.Int, kotlin.Int>>>?")!>y<!>
}

/*
 * TESTCASE NUMBER: 4
 * ISSUES: KT-17386
 */
fun case_4(x: Class?) {
    val y = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>if (x != null) x::fun_1 else 10<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
}

/*
 * TESTCASE NUMBER: 5
 * ISSUES: KT-17386
 */
fun case_5(x: Class?) {
    val y = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>if (x != null) Class::fun_1 else 10<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
}
