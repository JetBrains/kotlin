// MODULE: m1-common
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}, EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect interface Base<!>

// MODULE: m1-jvm()()(m1-common)
actual interface <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Base<!> {
    override fun equals(other: Any?): Boolean
}
