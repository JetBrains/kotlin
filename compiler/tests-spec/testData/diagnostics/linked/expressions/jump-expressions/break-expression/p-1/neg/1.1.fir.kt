// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case1() {
    val inputList = listOf(1, 2, 3)
    inputList.forEach {
        listOf("1.", "2.", "3.").forEach {
            if (true) <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>break@forEach<!>
        }
    }
}

// TESTCASE NUMBER: 2
fun case2() {
    val inputList = listOf(1, 2, 3)
    inputList.forEach {
        listOf("1.", "2.", "3.").forEach {
            if (true) <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>break<!>
        }
    }
}

// TESTCASE NUMBER: 3
fun case3() {
    <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>break<!>
}
