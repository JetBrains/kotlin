// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
// UNEXPECTED BEHAVIOUR
// ISSUES : KT-35545
fun case1(a: Boolean) = run { println("d"); return true }

// TESTCASE NUMBER: 2
val case2
get() = run { println("d"); return true }
