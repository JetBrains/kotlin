// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-296
 * MAIN LINK: type-system, type-kinds, built-in-types, kotlin.any -> paragraph 1 -> sentence 1
 * PRIMARY LINKS: type-system, introduction-1 -> paragraph 7 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: The use of Boolean literals as the identifier (with backtick) in the class.
 * HELPERS: checkType, functions
 */

// TESTCASE NUMBER: 1
class Case1

fun case_1(x: Case1) {
    checkSubtype<Any>(x)
    val z: Any = Case1()
    funWithAnyArg(Case1())
}

// TESTCASE NUMBER: 2
interface Case2

fun case_2(x: Case2) {
    checkSubtype<Any>(x)
    fun z(): Any = x
    funWithAnyArg(x)
}

// TESTCASE NUMBER: 3
data class Case3(val x: Int)
val case_3_1: Any = Case3(11)

fun case_3(x: Case3) {
    checkSubtype<Any>(x)
    funWithAnyArg(Case3(10))
}

// TESTCASE NUMBER: 4
sealed class Case4

fun case_4(x: Case4) {
    checkSubtype<Any>(x)
    funWithAnyArg(x)
}

// TESTCASE NUMBER: 5
annotation class Case5 {
    class Case5 {}
}

fun case_5(x: Case5, y: Case5.Case5) {
    checkSubtype<Any>(x)
    checkSubtype<Any>(y)
    val z1: Any = x
    val z2: Any = y
    funWithAnyArg(x)
    funWithAnyArg(Case5.Case5())
}

// TESTCASE NUMBER: 6
enum class Case6 {TEST;
    inner class Case6 {}
}
val case_6_1: Any = Case6.TEST.Case6()

fun case_6(x: Case6.Case6) {
    checkSubtype<Any>(x)
    funWithAnyArg(case_6_1)
}

// TESTCASE NUMBER: 7
enum class Case7 {;
    data class Case7(val x: Int) {}
}

fun case_7(x: Case7.Case7) {
    checkSubtype<Any>(x)
    val z: Any = Case7.Case7(10)
    funWithAnyArg(Case7.Case7(10))
}

// TESTCASE NUMBER: 8
enum class Case8 {;
    data class Case8(val x: Int) {}
}

typealias Case8_1 = Case8.Case8

fun case_8(x: Case8_1) {
    checkSubtype<Any>(x)
    val z: Any = Case8_1(10)
    funWithAnyArg(Case8_1(10))
}

// TESTCASE NUMBER: 9
interface Case9 {;
    data class Case9(val x: Int) {
        interface Case9
    }
}

val case_9_1: Any = object : Case9.Case9.Case9 {}
fun case_9(x: Case9.Case9.Case9) {
    checkSubtype<Any>(x)
    funWithAnyArg(object : Case9.Case9.Case9 {})
}
