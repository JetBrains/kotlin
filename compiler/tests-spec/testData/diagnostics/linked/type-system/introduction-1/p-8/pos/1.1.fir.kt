// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER
// !LANGUAGE: +NewInference
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1() {
    checkSubtype<Any?>(10)
}

// TESTCASE NUMBER: 2
fun case_2(x: String) {
    checkSubtype<Any?>(x)
}

// TESTCASE NUMBER: 3
class Case3(val x: String, val y: Any? = checkSubtype<Any?>(x))

// TESTCASE NUMBER: 4
class Case4(val x: String, val y: Any? = checkSubtype<Any?>(throw Exception()))

// TESTCASE NUMBER: 5
fun case_5() {
    checkSubtype<Any?>(return)
}

// TESTCASE NUMBER: 6
fun case_6() {
    while (true) {
        checkSubtype<Any?>(break)
    }
}

// TESTCASE NUMBER: 7
fun case_7(x: Boolean) {
    while (x) {
        checkSubtype<Any?>(continue)
    }
}

// TESTCASE NUMBER: 8
fun case_8(x: Boolean) {
    checkSubtype<Any?>({}())
}

// TESTCASE NUMBER: 9
fun case_9(x: Boolean) {
    checkSubtype<Any?>(kotlin.Unit)
}

// TESTCASE NUMBER: 10
fun case_10(x: Boolean) {
    checkSubtype<Any?>({while (true) {}}())
}

// TESTCASE NUMBER: 11
fun <T> case_11(x: T) {
    checkSubtype<Any?>(x)
}

// TESTCASE NUMBER: 12
class Case12<K> {
    inline fun <reified T: K> case_12(x: T) {
        checkSubtype<Any?>(x)
    }
}

// TESTCASE NUMBER: 13
class Case13<K> {
    inline fun <reified T: K> case_12(x: T) {
        x!!
        checkSubtype<Any?>(x)
    }
}
