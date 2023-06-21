// IGNORE_REVERSED_RESOLVE
// MODULE: common
// TARGET_PLATFORM: Common

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect class CommonClass {
    fun memberFun()
    val memberProp: Int
    class Nested
    inner class Inner
}<!>
actual class <!ACTUAL_WITHOUT_EXPECT!>CommonClass<!> {
    actual fun <!ACTUAL_WITHOUT_EXPECT!>memberFun<!>() {}
    actual val <!ACTUAL_WITHOUT_EXPECT!>memberProp<!>: Int = 42
    actual class <!ACTUAL_WITHOUT_EXPECT!>Nested<!>
    actual inner class <!ACTUAL_WITHOUT_EXPECT!>Inner<!>
}

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect fun commonFun()<!>
actual fun <!ACTUAL_WITHOUT_EXPECT!>commonFun<!>() {}

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect val commonProperty: String<!>
actual val <!ACTUAL_WITHOUT_EXPECT!>commonProperty<!>: String
    get() = "hello"

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common

expect class IntermediateClass {
    fun memberFun()
    val memberProp: Int
    class Nested
    inner class Inner
}
actual class <!ACTUAL_WITHOUT_EXPECT!>IntermediateClass<!> {
    actual fun <!ACTUAL_WITHOUT_EXPECT!>memberFun<!>() {}
    actual val <!ACTUAL_WITHOUT_EXPECT!>memberProp<!>: Int = 42
    actual class <!ACTUAL_WITHOUT_EXPECT!>Nested<!>
    actual inner class <!ACTUAL_WITHOUT_EXPECT!>Inner<!>
}

expect fun intermediateFun()
actual fun <!ACTUAL_WITHOUT_EXPECT!>intermediateFun<!>() {}

expect val intermediateProperty: String
actual val <!ACTUAL_WITHOUT_EXPECT!>intermediateProperty<!>: String
    get() = "hello"

// MODULE: main()()(intermediate)

expect class PlatformClass {
    fun memberFun()
    val memberProp: Int
    class Nested
    inner class Inner
}
actual class <!ACTUAL_WITHOUT_EXPECT!>PlatformClass<!> {
    actual fun <!ACTUAL_WITHOUT_EXPECT!>memberFun<!>() {}
    actual val <!ACTUAL_WITHOUT_EXPECT!>memberProp<!>: Int = 42
    actual class <!ACTUAL_WITHOUT_EXPECT!>Nested<!>
    actual inner class <!ACTUAL_WITHOUT_EXPECT!>Inner<!>
}

expect fun platformFun()
actual fun <!ACTUAL_WITHOUT_EXPECT!>platformFun<!>() {}

expect val platformProperty: String
actual val <!ACTUAL_WITHOUT_EXPECT!>platformProperty<!>: String
    get() = "hello"
