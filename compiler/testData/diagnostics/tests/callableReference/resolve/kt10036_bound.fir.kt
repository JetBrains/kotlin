// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER

class OverloadTest {
    fun foo(bar: Boolean) {}
    fun foo(bar: Any?) {}
}

inline fun <T : Any> OverloadTest.overload(value: T?, function: (T) -> Unit) {
}

fun OverloadTest.overloadBoolean(value: Boolean?) = overload(value, OverloadTest()::foo)
