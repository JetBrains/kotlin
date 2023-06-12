// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-222
 * MAIN LINK: expressions, jump-expressions, return-expressions -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION:
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1
fun case1() {
    val x = fooCase1()
    x checkType { check<Case1>() }
}

class Case1

fun fooCase1(): Case1 = TODO()

// TESTCASE NUMBER: 2
fun case2() {
    val x = fooCase2()
    x checkType { check<Case1>() }
}

fun fooCase2(): Case1 {
    val x = ""
    return Case1()
    val y = ""
}

// TESTCASE NUMBER: 3
fun case3() {
    val x = object : Case13 {
        override fun fooCase3(): Int {
            return 1
            "str"
            3
        }
    }.fooCase3()

    x checkType { check<Int>() }

}

interface Case13 {
    fun fooCase3(): Int
}

// TESTCASE NUMBER: 4
fun case4() {
    val x = fooCase4()
    x checkType { check<kotlin.Unit>() }
}

fun fooCase4()  {
    return
}
