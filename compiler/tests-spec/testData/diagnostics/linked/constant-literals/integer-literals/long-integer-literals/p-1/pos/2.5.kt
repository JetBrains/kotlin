// !CHECK_TYPE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, integer-literals, long-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 5
 * DESCRIPTION: Check of integer type selection depends on the context.
 */

// FILE: functions.kt

package functions

// TESTCASE NUMBER: 1
fun f1(x1: Byte, x2: Short, x3: Int, x4: Long) = x1 + x2 + x3 + x4

// TESTCASE NUMBER: 2
fun f2(x1: Short, x2: Int, x3: Long) = x1 + x2 + x3

// TESTCASE NUMBER: 3
fun f3(x1: Int, x2: Long) = x1 + x2

// TESTCASE NUMBER: 4
fun f4(x1: Long) = x1

// FILE: main.kt

import functions.*

// TESTCASE NUMBER: 1
fun case_1() {
    f1(0, 0, 0, 0)
    f1(127, 127, 127, 127)
    f1(-128, -128, -128, -128)
}

// TESTCASE NUMBER: 2
fun case_2() {
    f2(128, 128, 128)
    f2(-129, -129, -129)
    f2(32767, 32767, 32767)
    f2(-32768, -32768, -32768)
}

// TESTCASE NUMBER: 3
fun case_3() {
    f3(32768, 32768)
    f3(-32769, -32769)
    f3(2147483647, 2147483647)
    f3(-2147483648, -2147483648)
}

// TESTCASE NUMBER: 4
fun case_4() {
    f4(2147483648)
    f4(-2147483649)
    f4(9223372036854775807)
    f4(-9223372036854775807)
}
