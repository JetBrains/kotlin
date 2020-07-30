// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -REDUNDANT_PROJECTION
// SKIP_TXT


// TESTCASE NUMBER: 1
data class A(val x: Int, y: Int)

// TESTCASE NUMBER: 2
data class B<T>(val x: T, y: T)

// TESTCASE NUMBER: 3
data class C<T>(val x: T, y: List<out T>)

// TESTCASE NUMBER: 4
data class D<T>(x: T, val y: List<out T>)

// TESTCASE NUMBER: 5
data class E(val x: Int, vararg y: Int)

// TESTCASE NUMBER: 6
data class F<T>(val x: T, vararg y: T)

// TESTCASE NUMBER: 7
data class G<T>(val x: T, vararg y: List<out T>)

// TESTCASE NUMBER: 8
data class H<T>(x: T, vararg y: List<out T>)
