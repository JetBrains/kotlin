// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// FILE: KotlinClass.kt
// TESTCASE NUMBER: 1
fun case1() {
    val condition: Any = true
    while (condition && "true") {
    }
}

// TESTCASE NUMBER: 2
fun case2() {
    val condition: Boolean? = true
    while (condition) {
    }
}
