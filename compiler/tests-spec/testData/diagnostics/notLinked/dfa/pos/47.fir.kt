// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(a: Any?) {
    while (true) {
        if (a == null) return
        if (true) break
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>a<!>.equals(10)
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 */
fun case_2(a: Any?) {
    while (true) {
        if (a == null) continue
        if (true) break
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>a<!>.equals(10)
}

// TESTCASE NUMBER: 3
fun case_3(a: Any?) {
    while (true) {
        if (a == null) return continue
        if (true) break
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>a<!>.equals(10)
}

// TESTCASE NUMBER: 4
fun case_4(a: Any?) {
    while (true) {
        if (a == null) throw Exception()
        break
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>a<!>.equals(10)
}

// TESTCASE NUMBER: 5
fun case_5(a: Any?) {
    while (true) {
        if (a == null) return throw Exception()
        if (true) break
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>a<!>.equals(10)
}

// TESTCASE NUMBER: 6
fun case_6(a: Any?) {
    while (true) {
        if (a == null) return throw Exception()
        break
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>a<!>.equals(10)
}
