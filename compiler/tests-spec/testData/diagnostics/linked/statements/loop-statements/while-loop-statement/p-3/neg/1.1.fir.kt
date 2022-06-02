// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// FILE: KotlinClass.kt
// TESTCASE NUMBER: 1
fun case1() {
    val condition: Any = true
    while (<!CONDITION_TYPE_MISMATCH!>condition<!> && <!CONDITION_TYPE_MISMATCH!>"true"<!>) {
    }
}

// TESTCASE NUMBER: 2
fun case2() {
    val condition: Boolean? = true
    while (<!CONDITION_TYPE_MISMATCH!>condition<!>) {
    }
}
