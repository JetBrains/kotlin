// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, when-expression -> paragraph 3 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: The else condition must also be in the last when entry of when expression, otherwise it is a compile-time error
 */

// FILE: JavaEnum.java
enum JavaEnum {
    Val_1,
    Val_2,
    Val_3,
}

// FILE: KotlinClass.kt


// TESTCASE NUMBER: 1

fun case1() {
    val z = JavaEnum.Val_3
    val when1 = when (z) {
        JavaEnum.Val_1 -> { false }
        <!ELSE_MISPLACED_IN_WHEN!>else<!> -> {true}
        JavaEnum.Val_2 -> { false }
    }
}

// TESTCASE NUMBER: 2

fun case2() {
    val z = JavaEnum.Val_3
    val when1 = when (z) {
        <!ELSE_MISPLACED_IN_WHEN!>else<!> -> {true}
        JavaEnum.Val_1 -> { false }
        JavaEnum.Val_2 -> { false }
    }
}// TESTCASE NUMBER: 3

fun case3() {
    val z = JavaEnum.Val_3
    val when1 = when (z) {
        <!ELSE_MISPLACED_IN_WHEN!>else<!> -> {true}
        JavaEnum.Val_1 -> { false }
        JavaEnum.Val_2 -> { false }
        else -> { true }
    }
}
