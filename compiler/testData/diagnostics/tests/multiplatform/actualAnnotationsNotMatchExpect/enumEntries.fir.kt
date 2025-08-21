// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
annotation class Ann

<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>expect<!> enum class E {
    @Ann
    FOO,
    <!NO_ACTUAL_FOR_EXPECT{JVM}!>MISSING_ON_ACTUAL<!>
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual enum class <!EXPECT_ACTUAL_INCOMPATIBLE_ENUM_ENTRIES!>E<!> {
    <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>FOO<!>
}

/* GENERATED_FIR_TAGS: actual, annotationDeclaration, enumDeclaration, enumEntry, expect */
