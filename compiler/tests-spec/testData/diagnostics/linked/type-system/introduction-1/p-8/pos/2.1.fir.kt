// !DIAGNOSTICS: -UNUSED_VARIABLE -UNREACHABLE_CODE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-100
 * MAIN LINK: type-system, introduction-1 -> paragraph 8 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: The use of Boolean literals as the identifier (with backtick) in the class.
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1
fun case_1() {
    checkSubtype<Any?>(return)
}

// TESTCASE NUMBER: 2
fun case_2(x: Nothing) {
    checkSubtype<Nothing>(x)
}

// TESTCASE NUMBER: 3
class Case3(val x: Any? = checkSubtype<Nothing?>(throw Exception()))

// TESTCASE NUMBER: 4
class Case4(val x: Nothing, val y: Any? = checkSubtype<Int>(x))

// TESTCASE NUMBER: 5
fun case_5() {
    checkSubtype<Unit>(null!!)
}

// TESTCASE NUMBER: 6
fun <T> case_6() {
    while (true) {
        checkSubtype<Comparable<T>>(break)
    }
}

// TESTCASE NUMBER: 7
fun <T> case_7(x: Boolean) {
    while (x) {
        checkSubtype<T>(continue)
    }
}

// TESTCASE NUMBER: 8
class Case8<K> {
    inline fun <reified T: K, L: Nothing?> case_12(x: L) {
        x!!
        checkSubtype<K>(x)
    }
}

// TESTCASE NUMBER: 9
inline fun <reified L: Nothing?> case_9(x: L) = x!! as Int

// TESTCASE NUMBER: 10
fun <K> case_10(x: Nothing) = x as Iterable<K>

// TESTCASE NUMBER: 11
fun case_11() {
    return as Nothing?
}

// TESTCASE NUMBER: 12
fun case_12() = (throw Exception()) as String

// TESTCASE NUMBER: 13
fun case_13() = null as Float?

// TESTCASE NUMBER: 14
inline fun <reified T, K> case_14() {
    while (true) {
        break as Map<T, K>
    }
}

// TESTCASE NUMBER: 15
inline fun <reified T> case_15() {
    while (true) {
        continue as T
    }
}
