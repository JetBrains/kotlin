// MODULE: common
// TARGET_PLATFORM: Common
expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{COMMON}!>class A<!> {
    fun foo(x: String): String
}

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
expect class B

// K1 EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE false positive
actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class A<!> {
    actual fun foo(x: B) = "a"
}

// MODULE: main()()(intermediate)
actual typealias B = String
