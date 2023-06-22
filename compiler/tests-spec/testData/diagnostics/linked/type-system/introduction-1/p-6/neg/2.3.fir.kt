// !DIAGNOSTICS: -UNUSED_PARAMETER -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * MAIN LINK: type-system, introduction-1 -> paragraph 6 -> sentence 2
 * NUMBER: 3
 * DESCRIPTION: The use of Boolean literals as the identifier (with backtick) in the class.
 */

// TESTCASE NUMBER: 1
fun <T: Any> case_1(x: T = <!NULL_FOR_NONNULL_TYPE!>null<!>) {
    println(x)
}

// TESTCASE NUMBER: 2
fun <T: K, K: Number> case_2(x: T = <!NULL_FOR_NONNULL_TYPE!>null<!>) {
    println(x)
}

// TESTCASE NUMBER: 3
fun <K: Number> case_3() {
    class Case4<T: K> (x: T = <!NULL_FOR_NONNULL_TYPE!>null<!>)
}

// TESTCASE NUMBER: 4
fun <T> case_4() {
    class Case4(x: T = <!NULL_FOR_NONNULL_TYPE!>null<!>)
}

// TESTCASE NUMBER: 5
fun <T, K> case_5(x: T) where T: Number?, K: Number {
    if (x == null) {
        class Case5 constructor(y: K = <!INITIALIZER_TYPE_MISMATCH!>x<!>)
    }
}
