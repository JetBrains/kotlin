// MODULE: common
// TARGET_PLATFORM: Common

expect fun parameterCount()
fun parameterCount(p: String) {}

expect fun parameterCount2()
actual fun <!ACTUAL_WITHOUT_EXPECT!>parameterCount2<!>(p: String) {}

expect fun callableKind(): Int
val callableKind: Int = 1

expect fun <T> typeParameterCount()
fun typeParameterCount() {}

expect enum class <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>EnumEntries<!> {
    ONE, TWO;
}
actual enum class <!ACTUAL_WITHOUT_EXPECT, EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>EnumEntries<!> {
    ONE;
}

expect fun vararg(bar: Int)
fun vararg(vararg bar: Int) = Unit

// MODULE: main()()(common)

actual fun parameterCount() {}
actual fun <T> typeParameterCount() {}
actual fun callableKind(): Int = 1
actual fun vararg(bar: Int) = Unit
