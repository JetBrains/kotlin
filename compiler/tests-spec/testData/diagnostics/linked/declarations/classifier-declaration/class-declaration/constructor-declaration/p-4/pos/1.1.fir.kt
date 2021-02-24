// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT


// TESTCASE NUMBER: 1
class C1(x1: Boolean, val x2: Boolean, var x3: Boolean)

// TESTCASE NUMBER: 2
class C2(x1: Boolean, var x2: Boolean, vararg var x3: Boolean)

// TESTCASE NUMBER: 3
class C3(x1: Boolean, vararg val x2: Boolean, var x3: Boolean)
