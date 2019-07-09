// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 65
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1(x: Class) {
    if (x.prop_13 !is String) return
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.String")!>x.prop_13<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>x.prop_13<!>.length
}

// TESTCASE NUMBER: 2
fun case_2(x: Any?) {
    if (x !is String) return
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.String")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.String"), DEBUG_INFO_SMARTCAST!>x<!>.length
}

// TESTCASE NUMBER: 3
fun case_3(x: Class) {
    if (x.prop_13 !is String) throw Exception()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.String")!>x.prop_13<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String"), DEBUG_INFO_SMARTCAST!>x.prop_13<!>.length
}

// TESTCASE NUMBER: 4
fun case_4(x: Any?) {
    if (x !is String) throw Exception()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.String")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.String"), DEBUG_INFO_SMARTCAST!>x<!>.length
}

// TESTCASE NUMBER: 5
fun case_5(x: Class) {
    if (x.prop_13 !is String?) throw Exception()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.String?")!>x.prop_13<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?"), DEBUG_INFO_SMARTCAST!>x.prop_13<!>?.length
}

// TESTCASE NUMBER: 6
fun case_6(x: Any?) {
    if (x !is String?) throw Exception()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.String?"), DEBUG_INFO_SMARTCAST!>x<!>?.length
}

// TESTCASE NUMBER: 7
fun case_7(x: Any?) {
    if (x !is Pair<*, *>) throw Exception()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Pair<*, *>")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Pair<*, *>"), DEBUG_INFO_SMARTCAST!>x<!>.first
}

// TESTCASE NUMBER: 8
fun case_8(x: Any?) {
    if (x !is Pair<*, *>?) return
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Pair<*, *>?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Pair<*, *>?"), DEBUG_INFO_SMARTCAST!>x<!>?.first
}

// TESTCASE NUMBER: 9
fun case_9(x: Any?) {
    when (x) {
        !is Pair<*, *> -> throw Exception()
        else -> {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Pair<*, *>")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Pair<*, *>"), DEBUG_INFO_SMARTCAST!>x<!>.first
        }
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Pair<*, *>")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Pair<*, *>"), DEBUG_INFO_SMARTCAST!>x<!>.first
}

// TESTCASE NUMBER: 10
fun case_10(x: Any?) {
    when (x) {
        !is Pair<*, *>? -> return
        else -> {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Pair<*, *>?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Pair<*, *>?"), DEBUG_INFO_SMARTCAST!>x<!>?.first
        }
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Pair<*, *>?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Pair<*, *>?"), DEBUG_INFO_SMARTCAST!>x<!>?.first
}

// TESTCASE NUMBER: 11
fun case_11(x: Any?) {
    when {
        x !is Pair<*, *> -> throw Exception()
        else -> {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Pair<*, *>")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Pair<*, *>"), DEBUG_INFO_SMARTCAST!>x<!>.first
        }
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Pair<*, *>")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Pair<*, *>"), DEBUG_INFO_SMARTCAST!>x<!>.first
}

// TESTCASE NUMBER: 12
fun case_12(x: Any?) {
    when {
        x !is Pair<*, *>? -> return
        else -> {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Pair<*, *>?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Pair<*, *>?"), DEBUG_INFO_SMARTCAST!>x<!>?.first
        }
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Pair<*, *>?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Pair<*, *>?"), DEBUG_INFO_SMARTCAST!>x<!>?.first
}
