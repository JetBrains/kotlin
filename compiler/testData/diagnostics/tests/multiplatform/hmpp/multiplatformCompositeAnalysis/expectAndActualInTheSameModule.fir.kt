// IGNORE_REVERSED_RESOLVE
// MODULE: common
// TARGET_PLATFORM: Common

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect class CommonClass {
    <!NO_ACTUAL_FOR_EXPECT{JVM}!>fun memberFun()<!>
    <!NO_ACTUAL_FOR_EXPECT{JVM}!>val memberProp: Int<!>
    <!NO_ACTUAL_FOR_EXPECT{JVM}!>class Nested<!>
    <!NO_ACTUAL_FOR_EXPECT{JVM}!>inner class Inner<!>
}<!>
<!ACTUAL_WITHOUT_EXPECT!>actual class CommonClass {
    <!ACTUAL_WITHOUT_EXPECT!>actual fun memberFun() {}<!>
    <!ACTUAL_WITHOUT_EXPECT!>actual val memberProp: Int = 42<!>
    <!ACTUAL_WITHOUT_EXPECT!>actual class Nested<!>
    <!ACTUAL_WITHOUT_EXPECT!>actual inner class Inner<!>
}<!>

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect fun commonFun()<!>
<!ACTUAL_WITHOUT_EXPECT!>actual fun commonFun() {}<!>

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect val commonProperty: String<!>
<!ACTUAL_WITHOUT_EXPECT!>actual val commonProperty: String
    get() = "hello"<!>

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common

expect class IntermediateClass {
    fun memberFun()
    val memberProp: Int
    class Nested
    inner class Inner
}
<!ACTUAL_WITHOUT_EXPECT!>actual class IntermediateClass {
    <!ACTUAL_WITHOUT_EXPECT!>actual fun memberFun() {}<!>
    <!ACTUAL_WITHOUT_EXPECT!>actual val memberProp: Int = 42<!>
    <!ACTUAL_WITHOUT_EXPECT!>actual class Nested<!>
    <!ACTUAL_WITHOUT_EXPECT!>actual inner class Inner<!>
}<!>

expect fun intermediateFun()
<!ACTUAL_WITHOUT_EXPECT!>actual fun intermediateFun() {}<!>

expect val intermediateProperty: String
<!ACTUAL_WITHOUT_EXPECT!>actual val intermediateProperty: String
    get() = "hello"<!>

// MODULE: main()()(intermediate)

expect class PlatformClass {
    fun memberFun()
    val memberProp: Int
    class Nested
    inner class Inner
}
<!ACTUAL_WITHOUT_EXPECT!>actual class PlatformClass {
    <!ACTUAL_WITHOUT_EXPECT!>actual fun memberFun() {}<!>
    <!ACTUAL_WITHOUT_EXPECT!>actual val memberProp: Int = 42<!>
    <!ACTUAL_WITHOUT_EXPECT!>actual class Nested<!>
    <!ACTUAL_WITHOUT_EXPECT!>actual inner class Inner<!>
}<!>

expect fun platformFun()
<!ACTUAL_WITHOUT_EXPECT!>actual fun platformFun() {}<!>

expect val platformProperty: String
<!ACTUAL_WITHOUT_EXPECT!>actual val platformProperty: String
    get() = "hello"<!>
