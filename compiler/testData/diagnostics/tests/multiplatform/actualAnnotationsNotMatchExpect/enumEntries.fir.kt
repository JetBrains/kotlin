// MODULE: m1-common
// FILE: common.kt
annotation class Ann

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect enum class E {
    @Ann
    FOO,
    <!NO_ACTUAL_FOR_EXPECT{JVM}!>MISSING_ON_ACTUAL<!>
}<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual enum class <!ACTUAL_WITHOUT_EXPECT!>E<!> {
    <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>FOO<!>
}
