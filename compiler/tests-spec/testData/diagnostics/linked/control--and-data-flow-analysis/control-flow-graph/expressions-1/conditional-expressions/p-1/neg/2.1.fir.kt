// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1

fun case1() {
    val b = true
    if (!b) {
        println("this is statement")
    }
    val statement = if (!b) { println("statement could not be assigned") }
}

// TESTCASE NUMBER: 2

fun case2() {
    val a = 1
    val b = 2
    if (a > b) a else ; //statement
    val expression: Any = if (a > b) a else ;
}
