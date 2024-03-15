// MODULE: common
// TARGET_PLATFORM: Common

expect class <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>CommonClass<!> {
    fun memberFun()
    val memberProp: Int
    class Nested
    inner class Inner
}
actual class <!ACTUAL_WITHOUT_EXPECT, EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>CommonClass<!> {
    actual fun <!ACTUAL_WITHOUT_EXPECT!>memberFun<!>() {}
    actual val <!ACTUAL_WITHOUT_EXPECT!>memberProp<!>: Int = 42
    actual class <!ACTUAL_WITHOUT_EXPECT!>Nested<!>
    actual inner class <!ACTUAL_WITHOUT_EXPECT!>Inner<!>
}

expect fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>commonFun<!>()
actual fun <!ACTUAL_WITHOUT_EXPECT, EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>commonFun<!>() {}

expect val <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>commonProperty<!>: String
actual val <!ACTUAL_WITHOUT_EXPECT, EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>commonProperty<!>: String
    get() = "hello"

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common

expect class <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>IntermediateClass<!> {
    fun memberFun()
    val memberProp: Int
    class Nested
    inner class Inner
}
actual class <!ACTUAL_WITHOUT_EXPECT, EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>IntermediateClass<!> {
    actual fun <!ACTUAL_WITHOUT_EXPECT!>memberFun<!>() {}
    actual val <!ACTUAL_WITHOUT_EXPECT!>memberProp<!>: Int = 42
    actual class <!ACTUAL_WITHOUT_EXPECT!>Nested<!>
    actual inner class <!ACTUAL_WITHOUT_EXPECT!>Inner<!>
}

expect fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>intermediateFun<!>()
actual fun <!ACTUAL_WITHOUT_EXPECT, EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>intermediateFun<!>() {}

expect val <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>intermediateProperty<!>: String
actual val <!ACTUAL_WITHOUT_EXPECT, EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>intermediateProperty<!>: String
    get() = "hello"

// MODULE: main()()(intermediate)

expect class <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>PlatformClass<!> {
    fun memberFun()
    val memberProp: Int
    class Nested
    inner class Inner
}
actual class <!ACTUAL_WITHOUT_EXPECT, EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>PlatformClass<!> {
    actual fun <!ACTUAL_WITHOUT_EXPECT!>memberFun<!>() {}
    actual val <!ACTUAL_WITHOUT_EXPECT!>memberProp<!>: Int = 42
    actual class <!ACTUAL_WITHOUT_EXPECT!>Nested<!>
    actual inner class <!ACTUAL_WITHOUT_EXPECT!>Inner<!>
}

expect fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>platformFun<!>()
actual fun <!ACTUAL_WITHOUT_EXPECT, EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>platformFun<!>() {}

expect val <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>platformProperty<!>: String
actual val <!ACTUAL_WITHOUT_EXPECT, EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>platformProperty<!>: String
    get() = "hello"
