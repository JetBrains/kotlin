// MODULE: common
// TARGET_PLATFORM: Common
expect class A {
    fun foo(x: String): String
}

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
expect class B

// K1 EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE false positive
actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS{METADATA}!>A<!> {
    actual fun <!ACTUAL_WITHOUT_EXPECT, ACTUAL_WITHOUT_EXPECT{METADATA}!>foo<!>(x: B) = "a"
}

// MODULE: main()()(intermediate)
actual typealias B = String
