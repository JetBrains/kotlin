// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER
// !LANGUAGE: +NewInference
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1() {
    val x: Int = null
}

// TESTCASE NUMBER: 2
fun case_2() {
    val x: Any = null
}

// TESTCASE NUMBER: 3
fun case_3() {
    val x: Nothing = null
}

// TESTCASE NUMBER: 4
fun case_4() {
    var x: Int = null
}

// TESTCASE NUMBER: 5
fun case_5() {
    var x: Any = null
}

// TESTCASE NUMBER: 6
fun case_6() {
    var x: Nothing = null
}

// TESTCASE NUMBER: 7
fun case_7() {
    val x: Int
    x = null
}

// TESTCASE NUMBER: 8
fun case_8() {
    var x: Int = 10
    x = null
}

// TESTCASE NUMBER: 9
fun case_9() {
    val x = null
    val y: Int = x
}

// TESTCASE NUMBER: 10
fun case_10(x: Int?) {
    var y = 10
    y = x
}

// TESTCASE NUMBER: 11
fun case_11(x: Int?, y: Int = x) = null
