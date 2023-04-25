// MODULE: common
// TARGET_PLATFORM: Common

expect fun parameterCount()
fun parameterCount(p: String) {}

expect fun parameterCount2()
actual fun parameterCount2(p: String) {}

expect fun callableKind(): Int
val callableKind: Int = 1

expect fun <T> typeParameterCount()
fun typeParameterCount() {}

expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>enum class EnumEntries<!> {
    ONE, TWO;
}
actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>enum class EnumEntries<!> {
    ONE;
}

expect fun vararg(bar: Int)
fun vararg(vararg bar: Int) = Unit

// MODULE: main()()(common)

actual fun parameterCount() {}
actual fun <T> typeParameterCount() {}
actual fun callableKind(): Int = 1
actual fun vararg(bar: Int) = Unit
