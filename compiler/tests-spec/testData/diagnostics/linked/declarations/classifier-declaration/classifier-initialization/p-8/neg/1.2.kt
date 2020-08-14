// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, classifier-initialization -> paragraph 8 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: entry initialization an enum class
 */

// TESTCASE NUMBER: 1
enum class Case1 {
    VAL1,
    VAL2;

    init {
        <!UNINITIALIZED_ENUM_ENTRY!>VAL1<!> // UNINITIALIZED_ENUM_ENTRY
    }
}

// TESTCASE NUMBER: 2
enum class Case2 {
    VAL1,
    VAL2;

    init {
        Case2.<!UNINITIALIZED_ENUM_ENTRY!>VAL1<!> // UNINITIALIZED_ENUM_ENTRY
    }
}
