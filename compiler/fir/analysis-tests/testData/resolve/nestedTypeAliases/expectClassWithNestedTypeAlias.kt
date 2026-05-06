// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-85656
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

<!WRONG_MODIFIER_TARGET!>expect<!> typealias TopLevelExpectTA = String

expect class My {
    <!EXPECTED_TYPEALIAS!>typealias Numbers = List<Int><!> // Missing diagnostic about phobition of nested typealiases in expect classes
}

// MODULE: platform()()(common)
// FILE: platform.kt

actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>My<!>

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, typeAliasDeclaration */
