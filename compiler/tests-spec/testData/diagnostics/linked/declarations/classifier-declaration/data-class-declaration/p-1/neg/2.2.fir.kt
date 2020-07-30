// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
data class A(val x: Nothing, y: Nothing = TODO())

// TESTCASE NUMBER: 2
data class B(val x: Any, y: Any = 1)

// TESTCASE NUMBER: 3
data class C(val x: Any = TODO(), y: Any = 1)

// TESTCASE NUMBER: 4
data class D(val x: Any = TODO(), y: Any = 1)
