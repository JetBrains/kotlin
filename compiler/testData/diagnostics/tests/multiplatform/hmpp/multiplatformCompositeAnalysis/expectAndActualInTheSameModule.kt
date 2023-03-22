// IGNORE_REVERSED_RESOLVE
// MODULE: common
// TARGET_PLATFORM: Common

expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class CommonClass<!> {
    fun memberFun()
    val memberProp: Int
    class Nested
    inner class Inner
}
actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class CommonClass<!> {
    actual fun memberFun() {}
    actual val memberProp: Int = 42
    actual class Nested
    actual inner class Inner
}

expect fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>commonFun<!>()
actual fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>commonFun<!>() {}

expect val <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>commonProperty<!>: String
actual val <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>commonProperty<!>: String
    get() = "hello"

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common

expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class IntermediateClass<!> {
    fun memberFun()
    val memberProp: Int
    class Nested
    inner class Inner
}
actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class IntermediateClass<!> {
    actual fun memberFun() {}
    actual val memberProp: Int = 42
    actual class Nested
    actual inner class Inner
}

expect fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>intermediateFun<!>()
actual fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>intermediateFun<!>() {}

expect val <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>intermediateProperty<!>: String
actual val <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>intermediateProperty<!>: String
    get() = "hello"

// MODULE: main()()(intermediate)

expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class PlatformClass<!> {
    fun memberFun()
    val memberProp: Int
    class Nested
    inner class Inner
}
actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class PlatformClass<!> {
    actual fun memberFun() {}
    actual val memberProp: Int = 42
    actual class Nested
    actual inner class Inner
}

expect fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>platformFun<!>()
actual fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>platformFun<!>() {}

expect val <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>platformProperty<!>: String
actual val <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>platformProperty<!>: String
    get() = "hello"
