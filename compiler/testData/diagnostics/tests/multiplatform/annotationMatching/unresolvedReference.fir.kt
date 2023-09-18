// DIAGNOSTICS: -UNRESOLVED_REFERENCE
// MODULE: m1-common
// FILE: common.kt
@NonExistingClass
expect fun foo()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>foo<!>() {}
