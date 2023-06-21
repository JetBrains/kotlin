// MODULE: common
// TARGET_PLATFORM: Common

expect fun parameterCount()
fun parameterCount(p: String) {}

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect fun parameterCount2()<!>
actual fun <!ACTUAL_WITHOUT_EXPECT!>parameterCount2<!>(p: String) {}

expect fun callableKind(): Int
val callableKind: Int = 1

expect fun <T> typeParameterCount()
fun typeParameterCount() {}

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect enum class EnumEntries {
    ONE, TWO;
}<!>
actual enum class <!ACTUAL_WITHOUT_EXPECT!>EnumEntries<!> {
    ONE;
}

expect fun vararg(bar: Int)
fun vararg(vararg bar: Int) = Unit

// MODULE: main()()(common)

actual fun parameterCount() {}
actual fun <T> typeParameterCount() {}
actual fun callableKind(): Int = 1
actual fun vararg(bar: Int) = Unit
