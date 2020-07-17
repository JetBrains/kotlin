// !LANGUAGE: +NewInference
// !DIAGNOSTICS:  -IMPLICIT_CAST_TO_ANY -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1

fun case1() {
    val b = true
    val c = true
    val a = if (b) {
        "first true"
    } else if (c) {
        "else if true"
    }
}

// TESTCASE NUMBER: 2

fun case2() {
    var b = true
    val c = true
    val a = if (b) 1 else if (c) 2 else ;
}
