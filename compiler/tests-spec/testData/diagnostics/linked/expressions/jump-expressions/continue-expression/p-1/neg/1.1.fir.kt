// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-222
 * MAIN LINK: expressions, jump-expressions, continue-expression -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: A continue expression is a jump expression allowed only within loop bodies.
 */


// TESTCASE NUMBER: 1
fun case1() {
    val inputList = listOf(1, 2, 3)
    inputList.forEach {
        listOf("1.", "2.", "3.").forEach {
            if (true) <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>continue@forEach<!>
        }
    }
}

// TESTCASE NUMBER: 2
fun case2() {
    val inputList = listOf(1, 2, 3)
    inputList.forEach {
        listOf("1.", "2.", "3.").forEach {
            if (true) <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>continue<!>
        }
    }
}

// TESTCASE NUMBER: 3
fun case3() {
    <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>continue<!>
}
