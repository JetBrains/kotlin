// !DIAGNOSTICS: -UNUSED_PARAMETER -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

// TESTCASE NUMBER: 1
fun <T: Any> case_1(x: T = null) {
    println(x)
}

// TESTCASE NUMBER: 2
fun <T: K, K: Number> case_2(x: T = null) {
    println(x)
}

// TESTCASE NUMBER: 3
fun <K: Number> case_3() {
    class Case4<T: K> (x: T = null)
}

// TESTCASE NUMBER: 4
fun <T> case_4() {
    class Case4(x: T = null)
}

// TESTCASE NUMBER: 5
fun <T, K> case_5(x: T) where T: Number?, K: Number {
    if (x == null) {
        class Case5 constructor(y: K = x)
    }
}
